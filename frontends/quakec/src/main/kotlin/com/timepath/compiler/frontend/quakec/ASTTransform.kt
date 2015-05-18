package com.timepath.compiler.frontend.quakec

import com.timepath.Logger
import com.timepath.compiler.api.SymbolTable
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.data.Vector
import com.timepath.compiler.backend.q1vm.evaluate
import com.timepath.compiler.backend.q1vm.type
import com.timepath.compiler.backend.q1vm.types.*
import com.timepath.compiler.frontend.quakec.QCParser.DeclarationSpecifierContext
import com.timepath.compiler.frontend.quakec.QCParser.DeclaratorContext
import com.timepath.compiler.frontend.quakec.QCParser.ParameterTypeListContext
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t
import com.timepath.compiler.types.defaults.struct_t
import com.timepath.unquote
import com.timepath.with

private class ASTTransform(val state: Q1VM.State) : QCBaseVisitor<List<Expression>>() {

    companion object {
        val logger = Logger()
    }

    fun emptyList<T>(): List<T> = arrayListOf()
    fun listOf<T>(): MutableList<T> = arrayListOf()
    fun listOf<T>(vararg values: T): List<T> = arrayListOf(*values)

    inline fun match<T : Any, R>(it: T?, body: (T) -> R) = when (it) {
        null -> null
        is List<*> -> when {
            it.isEmpty() -> null
            else -> body(it)
        }
        else -> body(it)
    }

    inline fun SymbolTable.scope<R>(name: String, block: () -> R): R {
        push(name)
        val b = block()
        pop()
        return b
    }

    fun QCParser.DeclarationSpecifiersContext?.type(old: Boolean = false) = this?.let { type(it.declarationSpecifier(), old) }
    fun QCParser.DeclarationSpecifiers2Context?.type(old: Boolean = false) = this?.let { type(it.declarationSpecifier(), old) }
    fun type(list: List<DeclarationSpecifierContext>, old: Boolean = false) = list.lastOrNull { it.typeSpecifier() != null }?.let { type(it, old) }
    fun type(decl: DeclarationSpecifierContext, old: Boolean = false): Type? {
        val typeSpec = decl.typeSpecifier()
        val indirection = match(typeSpec.pointer()) { it.getText().length() } ?: 0
        return when {
        // varargs
            indirection == 3 -> void_t
            else -> state.types[typeSpec.directTypeSpecifier().children[0].getText()]!!
        }.let { direct ->
            when {
                old -> null
                else -> match(typeSpec.directTypeSpecifier().parameterTypeList()) { it.functionType(direct) }
            } ?: direct
        }.let { indirection.indices.fold(it) { it, _ -> field_t(it) } }
    }

    fun ParameterTypeListContext?.functionType(type: Type) = this?.let {
        val argTypes = match(it.parameterList()?.parameterDeclaration()) {
            it.map { it.declarationSpecifiers()?.type() ?: it.declarationSpecifiers2()?.type()!! }
        }?.let {
            if (it.singleOrNull() != void_t) it
            else null
        } ?: emptyList()
        if (argTypes.count { it == void_t } > 1) {
            throw UnsupportedOperationException("Multiple void parameters specified")
        }
        val vararg = match(it.parameterVarargs()) { it.declarationSpecifiers()?.type() }
        function_t(type, argTypes, vararg)
    }

    fun ParameterTypeListContext?.functionArgs() = match(this?.parameterList()?.parameterDeclaration()) {
        it.mapIndexed { i, it ->
            val type = (it.declarationSpecifiers()?.type() // named
                    ?: it.declarationSpecifiers2()?.type() // anonymous
                    )!!
            match(it.declarator()) {
                ParameterExpression(it.getText(), type, i, ctx = it)
            }
        }.filterNotNull()
    }

    fun ParameterTypeListContext?.functionVararg(): DeclarationExpression? {
        match(this?.parameterVarargs()) { // 'type?...'
            return DeclarationExpression(id = it.Identifier()?.getText() ?: "va_count"
                    , type = it.declarationSpecifiers()?.type() ?: void_t,
                    ctx = it)
        }
        // And now the old style functions... (and builtins)
        // FIXME: this is a mess
        return match(this?.parameterList()?.parameterDeclaration()) {
            // 'type ... id'
            val vararg = it.last()
            val specifiers = vararg.declarationSpecifiers2()?.declarationSpecifier()
            if (specifiers == null) return null
            check(specifiers.size() <= 2)
            val type = when {
                specifiers.size() == 2 -> type(specifiers.first(), false)!!
                else -> void_t
            }
            val id = specifiers.last().typeSpecifier().directTypeSpecifier().typedefName()?.let { it.Identifier().getText() }
            if (id == null) return null
            DeclarationExpression(id, type, ctx = specifiers.first())
        }
    }

    fun DeclaratorContext.deepest() = sequence(this) { it.declarator() }.last()

    override fun defaultResult() = emptyList<Expression>()

    override fun aggregateResult(aggregate: List<Expression>, nextResult: List<Expression>): List<Expression> {
        (aggregate as MutableList).addAll(nextResult)
        return aggregate
    }

    override fun visitCompilationUnit(ctx: QCParser.CompilationUnitContext) =
            BlockExpression(
                    add = /*state.symbols.scope("file") {*/visitChildren(ctx)/*}*/,
                    ctx = ctx).let { listOf(it) }

    override fun visitCompoundStatement(ctx: QCParser.CompoundStatementContext) =
            BlockExpression(
                    add = state.symbols.scope("block") { visitChildren(ctx) },
                    ctx = ctx).let { listOf(it) }

    override fun visitFunctionDefinition(ctx: QCParser.FunctionDefinitionContext): List<Expression> {
        val declarator = ctx.declarator()
        val old = declarator.parameterTypeList() == null
        val declSpecs = ctx.declarationSpecifiers()
        val parameterTypeList = when {
            old -> {
                val specs = declSpecs!!.declarationSpecifier()
                val typeSpec = specs.last { it.typeSpecifier() != null }.typeSpecifier()
                typeSpec.directTypeSpecifier().parameterTypeList()
            }
            else -> declarator.parameterTypeList()
        }!!
        val type = parameterTypeList.functionType(declSpecs.type(old)!!)!!
        val params = parameterTypeList.functionArgs()
        val vararg = parameterTypeList.functionVararg()
        val id = declarator.deepest().getText()
        val doChildren = fun FunctionExpression.() {
            state.symbols.declare(this)
            state.symbols.scope("params") {
                params?.forEach { state.symbols.declare(it) }
                vararg?.let { state.symbols.declare(DeclarationExpression(it.id, int_t, ctx = ctx)) }
                state.symbols.scope("body") {
                    addAll(visitChildren(ctx.compoundStatement()))
                }
            }
        }
        // Accumulate functions
        // TODO: check [[accumulate]] and [[last]]
        (state.symbols.resolve(id) as? FunctionExpression)?.let {
            it.doChildren()
            return listOf()
        }
        FunctionExpression(
                id = id,
                type = type,
                params = params,
                vararg = vararg,
                ctx = ctx
        ).let {
            it.doChildren()
            return listOf(it)
        }
    }

    override fun visitDeclaration(ctx: QCParser.DeclarationContext): List<Expression> {
        ctx.classSpecifier()?.let {
            val s = it.name.getText()
            if (state.types[s] == null) {
                val clazz = entity_t.extend(s)
                state.types[s] = clazz
            }
            return emptyList()
        }
        val declarations = ctx.initDeclaratorList()?.initDeclarator()
        if (declarations == null) {
            return ctx.enumSpecifier().enumeratorList().enumerator().mapTo(listOf<Expression>()) {
                val id = it.enumerationConstant().getText()
                int_t.declare(id, state = state).single().let { state.symbols.declare(it) }
            }
        }
        val specifiers = ctx.declarationSpecifiers().declarationSpecifier()
        specifiers.firstOrNull { it.storageClassSpecifier()?.getText() == "typedef" }?.let {
            val type = type(specifiers)!!
            declarations.forEach { state.types[it.getText()] = type }
            return emptyList()
        }
        val type = ctx.declarationSpecifiers().type()!!
        return declarations.flatMapTo(listOf<Expression>()) {
            val id = it.declarator().deepest().getText()
            val initializer = it.initializer()?.accept(this)?.single()
            val arraySize = it.declarator().assignmentExpression()
            when {
                initializer is Expression -> {
                    val value = initializer.evaluate(state)
                    when (value) {
                        null -> {
                            type.declare(id, state = state).flatMap {
                                listOf(it,
                                        BinaryExpression.Assign(
                                                left = ReferenceExpression(it as DeclarationExpression),
                                                right = initializer,
                                                ctx = ctx))
                            }
                        }
                        else -> {
                            // constant
                            val s = value.any.toString()
                            if (s.startsWith('#')) {
                                // FIXME: HACK
                                val i = s.substring(1).toInt()
                                // Similar to function definition
                                val old = it.declarator().parameterTypeList() == null
                                val parameterTypeList = when {
                                    old -> specifiers.last().typeSpecifier().directTypeSpecifier().parameterTypeList()
                                    else -> it.declarator().parameterTypeList()
                                }
                                val retType = ctx.declarationSpecifiers().type(old)
                                val params = parameterTypeList.functionArgs()
                                val vararg = parameterTypeList.functionVararg()
                                val signature = parameterTypeList.functionType(retType!!)!!
                                FunctionExpression(id, signature, params = params, vararg = vararg, builtin = i, ctx = ctx).let { listOf(it) }
                            } else {
                                type.declare(id, ConstantExpression(value), state = state)
                            }
                        }
                    }
                }
                arraySize != null -> {
                    val sizeExpr = arraySize.accept(this).single()
                    array_t(type, sizeExpr, state = state).declare(id, state = state)
                }
                else -> {
                    val ptl = it.declarator().parameterTypeList()
                    val params = ptl.functionArgs()
                    val vararg = ptl.functionVararg()
                    val signature = ptl.functionType(type) ?: type
                    val attribs = ctx.declarationSpecifiers().declarationSpecifier().sequence()
                            .flatMap { it.attributeList()?.let { it.attribute().sequence() } ?: sequenceOf() }
                            .filterNotNull()
                            .toList()
                    when (ptl) {
                        null -> when {
                            type is field_t && state.symbols.globalScope -> {
                                val extends = attribs.sequence().map {
                                    val classExtender = "class\\((.*)\\)".toRegex()
                                    val matcher = classExtender.matcher(it.getText())
                                    if (!matcher.matches()) return@map null
                                    state.types[matcher.group(1)] as? class_t
                                }.filterNotNull().toList()
                                (sequenceOf(entity_t) + extends).forEach {
                                    if (id in it.fields) {
                                        logger.warning { "redeclaring field $id" }
                                    }
                                    it.fields[id] = type.type
                                    state.fields[it, id]
                                }
                                emptyList<Expression>()
                            }
                            else -> type.declare(id, state = state)
                        }
                        else -> when {
                            signature is function_t -> // function prototype
                                FunctionExpression(id, signature, params = params, vararg = vararg, ctx = ctx).let { listOf(it) }
                            else -> // function pointer
                                signature.declare(id, state = state)
                        }
                    }
                }
            }.let {
                it.forEach { state.symbols.declare(it) }
                it
            }
        }
    }

    override fun visitCustomLabel(ctx: QCParser.CustomLabelContext) = with(listOf<Expression>()) {
        val id = ctx.Identifier().getText()
        add(LabelExpression(id, ctx = ctx))
        match(ctx.blockItem()) { addAll(it.accept(this@ASTTransform)) }
        this
    }

    override fun visitCaseLabel(ctx: QCParser.CaseLabelContext) = with(listOf<Expression>()) {
        val case = ctx.constantExpression().accept(this@ASTTransform).single()
        SwitchExpression.Case(case, ctx = ctx).let { add(it) }
        addAll(ctx.blockItem().accept(this@ASTTransform))
        this
    }

    override fun visitDefaultLabel(ctx: QCParser.DefaultLabelContext) = with(listOf<Expression>()) {
        add(SwitchExpression.Case(null, ctx = ctx))
        addAll(ctx.blockItem().accept(this@ASTTransform))
        this
    }

    override fun visitReturnStatement(ctx: QCParser.ReturnStatementContext) = ReturnStatement(
            match(ctx.expression()) { it.accept(this).single() },
            ctx = ctx).let { listOf(it) }

    override fun visitBreakStatement(ctx: QCParser.BreakStatementContext) = BreakStatement(
            ctx = ctx).let { listOf(it) }

    override fun visitContinueStatement(ctx: QCParser.ContinueStatementContext) = ContinueStatement(
            ctx = ctx).let { listOf(it) }

    override fun visitGotoStatement(ctx: QCParser.GotoStatementContext) = GotoExpression(
            id = ctx.Identifier().getText(),
            ctx = ctx).let { listOf(it) }

    override fun visitIterationStatement(ctx: QCParser.IterationStatementContext) = state.symbols.scope("loop") {
        val initializer = state.symbols.declare((ctx.initD ?: ctx.initE)?.accept(this))
        LoopExpression(
                predicate = when (ctx.predicate) {
                    null -> ConstantExpression(1, ctx = ctx)
                    else -> ctx.predicate.accept(this).single()
                },
                body = ctx.statement().accept(this).single(),
                checkBefore = ctx.getToken(QCParser.Do, 0) == null,
                initializer = initializer,
                update = ctx.update?.let { it.accept(this) },
                ctx = ctx).let { listOf(it) }
    }

    override fun visitIfStatement(ctx: QCParser.IfStatementContext): List<Expression> {
        val statements = ctx.statement()
        return ConditionalExpression(
                test = ctx.expression().accept(this).single().let {
                    if (ctx.getToken(QCParser.IfNot, 0) == null) it
                    else if (state.opts.ifNot) UnaryExpression.Not(it)
                    else throw UnsupportedOperationException("`if not (expr)` is disabled")
                },
                expression = false,
                pass = statements[0].accept(this).single(),
                fail = if (statements.size() == 1) null
                else statements[1].accept(this).single(),
                ctx = ctx).let { listOf(it) }
    }

    override fun visitSwitchStatement(ctx: QCParser.SwitchStatementContext) = SwitchExpression(
            test = ctx.expression().accept(this).single(),
            add = ctx.statement().accept(this),
            ctx = ctx).let { listOf(it) }

    override fun visitExpressionStatement(ctx: QCParser.ExpressionStatementContext): List<Expression> {
        match(ctx.expression()) { return it.accept(this) }
        return Nop(ctx = ctx).let { listOf(it) }
    }

    val QCParser.ExpressionContext.terminal: Boolean get() = expression() != null

    override fun visitExpression(ctx: QCParser.ExpressionContext) = when {
        ctx.terminal -> BinaryExpression.Comma(
                left = ctx.expression().accept(this).single(),
                right = ctx.assignmentExpression().accept(this).single(),
                ctx = ctx).let { listOf(it) }
        else -> super.visitExpression(ctx)
    }

    val QCParser.AssignmentExpressionContext.terminal: Boolean get() = assignmentExpression() != null

    override fun visitAssignmentExpression(ctx: QCParser.AssignmentExpressionContext) = when {
        ctx.terminal -> {
            val left = ctx.unaryExpression().accept(this).single()
            val right = ctx.assignmentExpression().accept(this).single()
            when (ctx.op.getType()) {
                QCParser.Assign -> BinaryExpression.Assign(left, right, ctx = ctx)
                QCParser.StarAssign -> BinaryExpression.Multiply.Assign(left, right, ctx = ctx)
                QCParser.DivAssign -> BinaryExpression.Divide.Assign(left, right, ctx = ctx)
                QCParser.ModAssign -> BinaryExpression.Modulo.Assign(left, right, ctx = ctx)
                QCParser.PlusAssign -> BinaryExpression.Add.Assign(left, right, ctx = ctx)
                QCParser.MinusAssign -> BinaryExpression.Subtract.Assign(left, right, ctx = ctx)
                QCParser.LeftShiftAssign -> BinaryExpression.Lsh.Assign(left, right, ctx = ctx)
                QCParser.RightShiftAssign -> BinaryExpression.Rsh.Assign(left, right, ctx = ctx)
                QCParser.AndAssign -> BinaryExpression.BitAnd.Assign(left, right, ctx = ctx)
                QCParser.XorAssign -> BinaryExpression.BitXor.Assign(left, right, ctx = ctx)
                QCParser.OrAssign -> BinaryExpression.BitOr.Assign(left, right, ctx = ctx)
                else -> throw NoWhenBranchMatchedException()
            }.let { listOf(it) }
        }
        else -> super.visitAssignmentExpression(ctx)
    }

    val QCParser.ConditionalExpressionContext.terminal: Boolean get() = expression().isNotEmpty()

    override fun visitConditionalExpression(ctx: QCParser.ConditionalExpressionContext) = when {
        ctx.terminal -> ConditionalExpression(
                test = ctx.logicalOrExpression().accept(this).single(),
                expression = true,
                pass = ctx.expression(0).accept(this).single(),
                fail = ctx.expression(1).accept(this).single(),
                ctx = ctx).let { listOf(it) }
        else -> super.visitConditionalExpression(ctx)
    }

    val QCParser.LogicalOrExpressionContext.terminal: Boolean get() = logicalOrExpression() != null

    override fun visitLogicalOrExpression(ctx: QCParser.LogicalOrExpressionContext) = when {
        ctx.terminal -> BinaryExpression.Or(
                left = ctx.logicalOrExpression().accept(this).single(),
                right = ctx.logicalAndExpression().accept(this).single(),
                ctx = ctx).let { listOf(it) }
        else -> super.visitLogicalOrExpression(ctx)
    }

    val QCParser.LogicalAndExpressionContext.terminal: Boolean get() = logicalAndExpression() != null

    override fun visitLogicalAndExpression(ctx: QCParser.LogicalAndExpressionContext) = when {
        ctx.terminal -> BinaryExpression.And(
                left = ctx.logicalAndExpression().accept(this).single(),
                right = ctx.inclusiveOrExpression().accept(this).single(),
                ctx = ctx).let { listOf(it) }
        else -> super.visitLogicalAndExpression(ctx)
    }

    val QCParser.InclusiveOrExpressionContext.terminal: Boolean get() = inclusiveOrExpression() != null

    override fun visitInclusiveOrExpression(ctx: QCParser.InclusiveOrExpressionContext) = when {
        ctx.terminal -> BinaryExpression.BitOr(
                left = ctx.inclusiveOrExpression().accept(this).single(),
                right = ctx.exclusiveOrExpression().accept(this).single(),
                ctx = ctx).let { listOf(it) }
        else -> super.visitInclusiveOrExpression(ctx)
    }

    val QCParser.ExclusiveOrExpressionContext.terminal: Boolean get() = exclusiveOrExpression() != null

    override fun visitExclusiveOrExpression(ctx: QCParser.ExclusiveOrExpressionContext) = when {
        ctx.terminal -> BinaryExpression.BitXor(
                left = ctx.exclusiveOrExpression().accept(this).single(),
                right = ctx.andExpression().accept(this).single(),
                ctx = ctx).let { listOf(it) }
        else -> super.visitExclusiveOrExpression(ctx)
    }

    val QCParser.AndExpressionContext.terminal: Boolean get() = andExpression() != null

    override fun visitAndExpression(ctx: QCParser.AndExpressionContext) = when {
        ctx.terminal -> BinaryExpression.BitAnd(
                left = ctx.andExpression().accept(this).single(),
                right = ctx.equalityExpression().accept(this).single(),
                ctx = ctx).let { listOf(it) }
        else -> super.visitAndExpression(ctx)
    }

    val QCParser.EqualityExpressionContext.terminal: Boolean get() = equalityExpression() != null

    override fun visitEqualityExpression(ctx: QCParser.EqualityExpressionContext) = when {
        ctx.terminal -> {
            val left = ctx.equalityExpression().accept(this).single()
            val right = ctx.relationalExpression().accept(this).single()
            when (ctx.op.getType()) {
                QCParser.Equal -> BinaryExpression.Eq(left, right, ctx = ctx)
                QCParser.NotEqual -> BinaryExpression.Ne(left, right, ctx = ctx)
                else -> throw NoWhenBranchMatchedException()
            }.let { listOf(it) }
        }
        else -> super.visitEqualityExpression(ctx)
    }

    val QCParser.RelationalExpressionContext.terminal: Boolean get() = relationalExpression() != null

    override fun visitRelationalExpression(ctx: QCParser.RelationalExpressionContext) = when {
        ctx.terminal -> {
            val left = ctx.relationalExpression().accept(this).single()
            val right = ctx.shiftExpression().accept(this).single()
            when (ctx.op.getType()) {
                QCParser.Less -> BinaryExpression.Lt(left, right, ctx = ctx)
                QCParser.LessEqual -> BinaryExpression.Le(left, right, ctx = ctx)
                QCParser.Greater -> BinaryExpression.Gt(left, right, ctx = ctx)
                QCParser.GreaterEqual -> BinaryExpression.Ge(left, right, ctx = ctx)
                else -> throw NoWhenBranchMatchedException()
            }.let { listOf(it) }
        }
        else -> super.visitRelationalExpression(ctx)
    }

    val QCParser.ShiftExpressionContext.terminal: Boolean get() = shiftExpression() != null

    override fun visitShiftExpression(ctx: QCParser.ShiftExpressionContext) = when {
        ctx.terminal -> {
            val left = ctx.shiftExpression().accept(this).single()
            val right = ctx.additiveExpression().accept(this).single()
            when (ctx.op.getType()) {
                QCParser.LeftShift -> BinaryExpression.Lsh(left, right, ctx = ctx)
                QCParser.RightShift -> BinaryExpression.Rsh(left, right, ctx = ctx)
                else -> throw NoWhenBranchMatchedException()
            }.let { listOf(it) }
        }
        else -> super.visitShiftExpression(ctx)
    }

    val QCParser.AdditiveExpressionContext.terminal: Boolean get() = additiveExpression() != null

    override fun visitAdditiveExpression(ctx: QCParser.AdditiveExpressionContext) = when {
        ctx.terminal -> {
            val left = ctx.additiveExpression().accept(this).single()
            val right = ctx.multiplicativeExpression().accept(this).single()
            when (ctx.op.getType()) {
                QCParser.Plus -> BinaryExpression.Add(left, right, ctx = ctx)
                QCParser.Minus -> BinaryExpression.Subtract(left, right, ctx = ctx)
                else -> throw NoWhenBranchMatchedException()
            }.let { listOf(it) }
        }
        else -> super.visitAdditiveExpression(ctx)
    }

    val QCParser.MultiplicativeExpressionContext.terminal: Boolean get() = multiplicativeExpression() != null

    override fun visitMultiplicativeExpression(ctx: QCParser.MultiplicativeExpressionContext) = when {
        ctx.terminal -> {
            val left = ctx.multiplicativeExpression().accept(this).single()
            val right = ctx.castExpression().accept(this).single()
            when (ctx.op.getType()) {
                QCParser.Star -> BinaryExpression.Multiply(left, right, ctx = ctx)
                QCParser.Div -> BinaryExpression.Divide(left, right, ctx = ctx)
                QCParser.Mod -> BinaryExpression.Modulo(left, right, ctx = ctx)
                QCParser.Cross -> throw UnsupportedOperationException("Vector cross (><) ${ctx.getText()}")
                else -> throw NoWhenBranchMatchedException()
            }.let { listOf(it) }
        }
        else -> super.visitMultiplicativeExpression(ctx)
    }

    val QCParser.CastExpressionContext.terminal: Boolean get() = castExpression() != null

    override fun visitCastExpression(ctx: QCParser.CastExpressionContext) = when {
        ctx.terminal -> {
            val type = state.types[ctx.typeName().getText()]!!
            val expr = ctx.castExpression().accept(this).single()
            UnaryExpression.Cast(
                    type = type,
                    operand = expr,
                    ctx = ctx
            ).let { listOf(it) }
        }
        else -> super.visitCastExpression(ctx)
    }

    val QCParser.UnaryExpressionContext.terminal: Boolean get() = unaryExpression() != null

    override fun visitUnaryExpression(ctx: QCParser.UnaryExpressionContext) = when {
        ctx.terminal -> {
            val expr = ctx.unaryExpression().accept(this).single()
            when (ctx.op.getType()) {
                QCParser.PlusPlus -> UnaryExpression.PreIncrement(expr, ctx = ctx)
                QCParser.MinusMinus -> UnaryExpression.PreDecrement(expr, ctx = ctx)
                QCParser.And -> UnaryExpression.Address(expr, ctx = ctx)
                QCParser.Star -> UnaryExpression.Dereference(expr, ctx = ctx)
                QCParser.Plus -> UnaryExpression.Plus(expr, ctx = ctx)
                QCParser.Minus -> UnaryExpression.Minus(expr, ctx = ctx)
                QCParser.Tilde -> UnaryExpression.BitNot(expr, ctx = ctx)
                QCParser.Not -> UnaryExpression.Not(expr, ctx = ctx)
                else -> throw NoWhenBranchMatchedException()
            }.let { listOf(it) }
        }
        else -> super.visitUnaryExpression(ctx)
    }

    override fun visitPostfixPrimary(ctx: QCParser.PostfixPrimaryContext) = ctx.primaryExpression().accept(this)

    override fun visitPostfixCall(ctx: QCParser.PostfixCallContext): List<Expression> {
        val left = ctx.postfixExpression().accept(this).single()
        val right = match(ctx.argumentExpressionList()) {
            it.assignmentExpression().map { it.accept(this).single() }
        } ?: emptyList()
        return MethodCallExpression(function = left, add = right, ctx = ctx).let { listOf(it) }
    }

    override fun visitPostfixVararg(ctx: QCParser.PostfixVarargContext): List<Expression> {
        val type = state.types[ctx.typeName().getText()]!!
        val va_args = state.symbols.resolve("VA_ARGS")!!
        val va_arg = MethodCallExpression(va_args, ctx.expression().accept(this).single().let { listOf(it) }, ctx = ctx)
        return UnaryExpression.Cast(type, va_arg, ctx = ctx).let { listOf(it) }
    }

    val matchVecComponent = "^(.+)_(x|y|z)$".toRegex()

    /**
     * static:
     * struct.field
     */
    override fun visitPostfixField(ctx: QCParser.PostfixFieldContext): List<Expression> {
        val left = ctx.postfixExpression().accept(this).single()
        val ltype = left.type(state)
        if (ltype !is struct_t) {
            throw UnsupportedOperationException("Applying field to non-struct type $ltype")
        }
        val text = ctx.Identifier().getText()
        val vecMatcher = matchVecComponent.matcher(text)
        return when {
            state.opts.legacyVectors && vecMatcher.matches() -> {
                // `ent.vec_x` -> `(ent.vec).x`
                // FIXME: hides similarly named fields which should probably shadow the vector
                val vector = vecMatcher.group(1)
                val component = vecMatcher.group(2)
                if (state.symbols.resolve(vector) != null) {
                    // This isn't just a `float vec_x`
                    MemberExpression(
                            left = MemberExpression(
                                    left = left
                                    , field = MemberReferenceExpression(ltype, vector)
                                    , ctx = ctx)
                            , field = MemberReferenceExpression(vector_t, component)
                            , ctx = ctx)
                } else {
                    // Use as written
                    MemberExpression(
                            left = left
                            , field = MemberReferenceExpression(ltype, text)
                            , ctx = ctx)
                }
            }
            else -> {
                val sym = state.symbols.resolve(text)
                val field = ltype.fields[text]
                when {
                    field != null -> // Favor fields
                        MemberExpression(left = left, field = MemberReferenceExpression(ltype, text), ctx = ctx)
                    sym != null -> // Fall back to locals and params
                        when {
                            sym.type is array_t -> // This is for PostfixIndex
                                MemberExpression(left = left, field = MemberReferenceExpression(ltype, text), ctx = ctx)
                            state.opts.legacyPointerToMember && sym.type is field_t -> IndexExpression(left = left, right = sym, ctx = ctx)
                            else -> throw UnsupportedOperationException("Applying $sym to struct type $ltype")
                        }
                    else -> throw NullPointerException("Can't resolve $ltype.$text")
                }
            }
        }.let { listOf(it) }
    }

    /**
     * dynamic:
     * entity.(field)
     */
    override fun visitPostfixAddress(ctx: QCParser.PostfixAddressContext): List<Expression> {
        val left = ctx.postfixExpression().accept(this).single()
        val right = ctx.expression().accept(this).single()
        return IndexExpression(left = left, right = right, ctx = ctx).let { listOf(it) }
    }

    /**
     * dynamic:
     * array[index]
     */
    override fun visitPostfixIndex(ctx: QCParser.PostfixIndexContext): List<Expression> {
        val left = ctx.postfixExpression().accept(this).single()
        val right = ctx.expression().accept(this).single()
        return when (left) {
            is MemberExpression -> {
                // (ent.arr)[i] -> ent.(arr[i])
                val field = ReferenceExpression(state.symbols.resolve(left.field.id)!!)
                IndexExpression(left = left.left, right = IndexExpression(field, right), ctx = ctx)
            }
            else -> IndexExpression(left = left, right = right, ctx = ctx)
        }.let { listOf(it) }
    }

    override fun visitPostfixIncr(ctx: QCParser.PostfixIncrContext): List<Expression> {
        val expr = ctx.postfixExpression().accept(this).single()
        return when (ctx.op.getType()) {
            QCParser.PlusPlus -> UnaryExpression.PostIncrement(expr, ctx = ctx)
            QCParser.MinusMinus -> UnaryExpression.PostDecrement(expr, ctx = ctx)
            else -> throw NoWhenBranchMatchedException()
        }.let { listOf(it) }
    }

    val matchChar = "'(.)'".toRegex()
    val matchVec = "'\\s*([+-]?[\\d.]+)\\s*([+-]?[\\d.]+)\\s*([+-]?[\\d.]+)\\s*'".toRegex()
    val matchHex = "0x([\\d0-F]+)".toRegex()

    override fun visitPrimaryExpression(ctx: QCParser.PrimaryExpressionContext): List<Expression> {
        val text = ctx.getText()
        match(ctx.Identifier()) {
            // TODO: other types?
            val e = entity_t
            run {
                val symbol = state.symbols.resolve(text)
                val member = e.fields[text]
                when {
                    symbol != null ->
                        return ReferenceExpression(
                                symbol,
                                ctx = ctx).let { listOf(it) }
                    state.opts.legacyFieldNamespace
                            && member != null ->
                        return MemberReferenceExpression(
                                e, text,
                                ctx = ctx).let { listOf(it) }
                }
                Unit
            }
            val matcher = matchVecComponent.matcher(text)
            if (state.opts.legacyVectors && matcher.matches()) {
                val vector = matcher.group(1)
                val component = matcher.group(2)
                val symbol = state.symbols.resolve(vector)
                val member = e.fields[vector]
                when {
                    symbol != null
                            && symbol.type is vector_t ->
                        return MemberExpression(
                                left = ReferenceExpression(symbol, ctx = ctx),
                                field = MemberReferenceExpression(vector_t, component, ctx = ctx),
                                ctx = ctx).let { listOf(it) }
                    state.opts.legacyFieldNamespace
                            && member != null
                            && member is vector_t ->
                        // Pointer to member of member is illegal, must use a union of the member and its members
                        return MemberReferenceExpression(e, text, ctx = ctx).let { listOf(it) }
                }
            }

            throw NullPointerException("Unable to resolve symbol $text")
        }
        match(ctx.Constant()) {
            val s = it.getText()
            matchChar.let {
                val matcher = it.matcher(s)
                if (matcher.matches()) {
                    return ConstantExpression(matcher.group(1).charAt(0), ctx = ctx).let { listOf(it) }
                }
            }
            matchVec.let {
                val matcher = it.matcher(s)
                if (matcher.matches()) {
                    val c1 = matcher.group(1).toFloat()
                    val c2 = matcher.group(2).toFloat()
                    val c3 = matcher.group(3).toFloat()
                    return ConstantExpression(Vector(c1, c2, c3), ctx = ctx).let { listOf(it) }
                }
            }
            matchHex.let {
                val matcher = it.matcher(s)
                if (matcher.matches()) {
                    val hex = Integer.parseInt(matcher.group(1), 16)
                    return ConstantExpression(hex.toFloat(), ctx = ctx).let { listOf(it) }
                }
            }
            if (s.startsWith('#')) {
                return ConstantExpression(text, ctx = ctx).let { listOf(it) }
            }
            val f = s.toFloat()
            val i = f.toInt()
            return (ConstantExpression(when (i.toFloat()) {
                f -> i
                else -> f
            }, ctx = ctx)).let { listOf(it) }
        }
        match(ctx.StringLiteral()) {
            return ConstantExpression(StringBuilder { it.forEach { append(it.getText().unquote()) } }.toString(),
                    ctx = ctx).let { listOf(it) }
        }
        match(ctx.expression()) {
            it.singleOrNull()?.let {
                return it.accept(this)
            }
            return listOf(BlockExpression(linkedListOf<Expression>().with {
                val decl = StructDeclarationExpression("tmp", vector_t)
                val vec = ReferenceExpression(decl)
                add(decl)
                val x = MemberExpression(vec, MemberReferenceExpression(vector_t, "x"))
                val y = MemberExpression(vec, MemberReferenceExpression(vector_t, "y"))
                val z = MemberExpression(vec, MemberReferenceExpression(vector_t, "z"))
                add(BinaryExpression.Assign(x, it[0].accept(this@ASTTransform).single()))
                add(BinaryExpression.Assign(y, it[1].accept(this@ASTTransform).single()))
                add(BinaryExpression.Assign(z, it[2].accept(this@ASTTransform).single()))
                add(vec)
            }, ctx = ctx))
        }
        throw UnsupportedOperationException(ctx.getText())
    }
}
