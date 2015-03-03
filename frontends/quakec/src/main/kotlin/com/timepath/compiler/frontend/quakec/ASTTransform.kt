package com.timepath.compiler.frontend.quakec

import com.timepath.Logger
import com.timepath.compiler.Vector
import com.timepath.compiler.ast.*
import com.timepath.compiler.frontend.quakec.QCParser.DeclarationSpecifierContext
import com.timepath.compiler.frontend.quakec.QCParser.ParameterTypeListContext
import com.timepath.compiler.gen.evaluate
import com.timepath.compiler.types.*
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.compiler.api.CompileState
import java.util.ArrayList
import java.util.Deque
import java.util.LinkedList
import java.util.LinkedHashMap
import com.timepath.compiler.frontend.quakec.QCParser.DeclaratorContext

class ASTTransform(val state: CompileState) : QCBaseVisitor<List<Expression>>() {

    [suppress("NOTHING_TO_INLINE")]
    inline fun emptyList<T>() = ArrayList<T>()

    [suppress("NOTHING_TO_INLINE")]
    inline fun listOf<T>() = ArrayList<T>()

    [suppress("NOTHING_TO_INLINE")]
    inline fun listOf<T>(vararg values: T) = ArrayList<T>(values.size()).let {
        it.addAll(values)
        it
    }

    data class Scope(val name: String, val vars: MutableMap<String, DeclarationExpression> = LinkedHashMap())

    val scope: Deque<Scope> = LinkedList()

    fun <R> scoped(name: String, block: () -> R): R {
        scope.push(Scope(name))
        try {
            return block()
        } finally {
            scope.pop()
        }
    }

    fun <R> declare(e: R): R {
        val vars = scope.peek().vars
        if (e is DeclarationExpression) {
            vars[e.id] = e
        }
        return e
    }

    fun resolve(id: String) = scope.first { id in it.vars }.vars[id]

    class object {
        val logger = Logger.new()
    }

    private fun debug(ctx: ParserRuleContext) {
        val token = ctx.start
        val source = token.getTokenSource()

        val line = token.getLine()
        val col = token.getCharPositionInLine()
        val file = source.getSourceName()
        logger.fine("{$token} $line,$col $file")
    }

    fun QCParser.DeclarationSpecifiersContext?.type(old: Boolean = false): Type? {
        if (this == null) return null
        return type(this.declarationSpecifier(), old)
    }

    fun QCParser.DeclarationSpecifiers2Context?.type(old: Boolean = false): Type? {
        if (this == null) return null
        return type(this.declarationSpecifier(), old)
    }

    fun type(list: List<DeclarationSpecifierContext>?, old: Boolean = false): Type? {
        val decl = list?.lastOrNull { it.typeSpecifier() != null }
        if (decl == null) return null
        val typeSpec = decl.typeSpecifier()
        val indirection = typeSpec.pointer()?.getText()?.length() ?: 0
        val spec = typeSpec.directTypeSpecifier()
        val functional = spec.parameterTypeList()
        val direct = when {
        // varargs
            typeSpec.pointer()?.getText()?.length() == 3 -> void_t
            else -> state.types[typeSpec.directTypeSpecifier().children[0].getText()]
        }
        var indirect = if (functional != null && !old) {
            functional.functionType(direct)
        } else {
            direct
        }
        indirection.times {
            indirect = field_t(indirect)
        }
        return indirect
    }

    fun ParameterTypeListContext?.functionType(type: Type): Type {
        if (this == null) return type
        val parameterList = this.parameterList()
        val parameterVarargs = this.parameterVarargs()
        val parameterDeclarations = parameterList?.parameterDeclaration()
        val typeArgs = parameterDeclarations?.map { it.declarationSpecifiers()?.type() ?: it.declarationSpecifiers2()?.type() } ?: emptyList()
        val vararg = parameterVarargs?.declarationSpecifiers()?.type()
        val function = function_t(type, typeArgs.requireNoNulls().filter { it != void_t }, vararg)
        return function
    }

    fun ParameterTypeListContext?.functionArgs(ctx: ParserRuleContext): List<Expression> {
        val paramDeclarations = this?.parameterList()?.parameterDeclaration() ?: emptyList()
        var i = 0
        val params = paramDeclarations.flatMapTo(listOf<Expression>()) {
            val paramId = it.declarator()?.getText()
            when (paramId) {
                null -> emptyList<Expression>()
                else -> {
                    val type = it.declarationSpecifiers().type()
                    val param = ParameterExpression(paramId, type!!, index = i++, ctx = ctx)
                    param.let { listOf(it) }
                }
            }
        }
        return params
    }

    fun ParameterTypeListContext?.functionVararg(ctx: ParserRuleContext) = this?.parameterVarargs()?.let {
        DeclarationExpression(
                id = it.Identifier()?.getText() ?: "...",
                type = it.declarationSpecifiers().type() ?: void_t,
                value = null,
                ctx = ctx)
    }

    override fun defaultResult() = emptyList<Expression>()

    override fun aggregateResult(aggregate: List<Expression>, nextResult: List<Expression>): List<Expression> {
        (aggregate as MutableList).addAll(nextResult)
        return aggregate
    }

    override fun visitCompilationUnit(ctx: QCParser.CompilationUnitContext) =
            BlockExpression(
                    add = scoped("file") { visitChildren(ctx) },
                    ctx = ctx).let { listOf(it) }

    override fun visitCompoundStatement(ctx: QCParser.CompoundStatementContext) =
            BlockExpression(
                    add = scoped("block") { visitChildren(ctx) },
                    ctx = ctx).let { listOf(it) }

    private fun DeclaratorContext.deepest(): DeclaratorContext {
        var declarator = this
        while (declarator.declarator() != null) {
            declarator = declarator.declarator()
        }
        return declarator
    }

    override fun visitFunctionDefinition(ctx: QCParser.FunctionDefinitionContext): List<Expression> {
        val declaratorContext = ctx.declarator()
        val old = declaratorContext.parameterTypeList() == null
        val parameterTypeList = when {
            old -> ctx.declarationSpecifiers().declarationSpecifier().last().typeSpecifier().directTypeSpecifier().parameterTypeList()
            else -> declaratorContext.parameterTypeList()
        }
        FunctionExpression(
                id = declaratorContext.deepest().getText(),
                type = parameterTypeList.functionType(ctx.declarationSpecifiers().type(old)!!) as function_t,
                params = parameterTypeList.functionArgs(ctx),
                vararg = parameterTypeList.functionVararg(ctx),
                ctx = ctx
        ).let {
            declare(it)
            scoped("params") {
                it.params?.forEach { declare(it) }
                it.vararg?.let { declare(it) }
                scoped("body") {
                    it.addAll(visitChildren(ctx.compoundStatement()))
                }
            }
            return listOf(it)
        }
    }

    override fun visitDeclaration(ctx: QCParser.DeclarationContext): List<Expression> {
        val declarations = ctx.initDeclaratorList()?.initDeclarator()
        if (declarations == null) {
            return ctx.enumSpecifier().enumeratorList().enumerator().mapTo(listOf<Expression>()) {
                val id = it.enumerationConstant().getText()
                int_t.declare(id).single().let { declare(it) }
            }
        }
        val specifiers = ctx.declarationSpecifiers().declarationSpecifier()
        specifiers.firstOrNull { it.storageClassSpecifier()?.getText() == "typedef" }?.let {
            val type = type(specifiers)!!
            declarations.forEach { state.types[it.getText()] = type }
            return emptyList()
        }
        val type = ctx.declarationSpecifiers().type()
        return declarations.flatMapTo(listOf<Expression>()) {
            val id = it.declarator().deepest().getText()
            val initializer = it.initializer()?.accept(this)?.single()
            when (initializer) {
                is Expression -> {
                    val value = initializer.evaluate()
                    when (value) {
                        null -> {
                            type!!.declare(id, state = state).flatMap {
                                listOf(it, BinaryExpression.Assign(ReferenceExpression(it as DeclarationExpression), initializer, ctx = ctx))
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
                                val params = parameterTypeList.functionArgs(ctx)
                                val vararg = parameterTypeList.functionVararg(ctx)
                                val signature = parameterTypeList.functionType(retType!!);
                                FunctionExpression(id, signature as function_t, params = params, vararg = vararg, builtin = i, ctx = ctx).let { listOf(it) }
                            } else {
                                type!!.declare(id, ConstantExpression(value), state = state)
                            }
                        }
                    }
                }
                else -> {
                    val arraySize = it.declarator().assignmentExpression()
                    if (arraySize != null) {
                        val sizeExpr = arraySize.accept(this).single()
                        array_t(type!!, sizeExpr).declare(id, state = state)
                    } else {
                        val old = it.declarator().parameterTypeList() == null
                        val parameterTypeList = if (old) {
                            specifiers.last().typeSpecifier().directTypeSpecifier().parameterTypeList()
                        } else {
                            it.declarator().parameterTypeList()
                        }
                        val retType = ctx.declarationSpecifiers().type(old)
                        val params = parameterTypeList.functionArgs(ctx)
                        val vararg = parameterTypeList.functionVararg(ctx)
                        val signature = parameterTypeList.functionType(retType!!.let {
                            when (retType) {
                                is field_t -> retType.type
                                else -> it
                            }
                        }).let {
                            when (retType) {
                                is field_t -> field_t(it)
                                else -> it
                            }
                        }
                        when (parameterTypeList) {
                            null ->
                                type!!.declare(id, state = state)
                            else -> when {
                                signature is function_t -> // function prototype
                                    FunctionExpression(id, signature, params = params, vararg = vararg, ctx = ctx).let { listOf(it) }
                                else -> // function pointer
                                    signature.declare(id, state = state)
                            }
                        }
                    }
                }
            }.let {
                it.forEach { declare(it) }
                it
            }
        }
    }

    override fun visitCustomLabel(ctx: QCParser.CustomLabelContext) = with(listOf<Expression>()) {
        val id = ctx.Identifier().getText()
        LabelExpression(id, ctx = ctx).let { add(it) }
        ctx.blockItem()?.accept(this@ASTTransform)?.let { addAll(it) }
        this
    }

    override fun visitCaseLabel(ctx: QCParser.CaseLabelContext) = with(listOf<Expression>()) {
        val case = ctx.constantExpression().accept(this@ASTTransform).single()
        SwitchExpression.Case(case, ctx = ctx).let { add(it) }
        ctx.blockItem()?.accept(this@ASTTransform)?.let { addAll(it) }
        this
    }

    override fun visitDefaultLabel(ctx: QCParser.DefaultLabelContext) = with(listOf<Expression>()) {
        val default = null
        SwitchExpression.Case(default, ctx = ctx).let { add(it) }
        ctx.blockItem()?.accept(this@ASTTransform)?.let { addAll(it) }
        this
    }

    override fun visitReturnStatement(ctx: QCParser.ReturnStatementContext) = ReturnStatement(
            ctx.expression()?.accept(this)?.single(),
            ctx = ctx).let { listOf(it) }

    override fun visitBreakStatement(ctx: QCParser.BreakStatementContext) = BreakStatement(
            ctx = ctx).let { listOf(it) }

    override fun visitContinueStatement(ctx: QCParser.ContinueStatementContext) = ContinueStatement(
            ctx = ctx).let { listOf(it) }

    override fun visitGotoStatement(ctx: QCParser.GotoStatementContext) = GotoExpression(
            id = ctx.Identifier().getText(),
            ctx = ctx).let { listOf(it) }

    override fun visitIterationStatement(ctx: QCParser.IterationStatementContext) = scoped("loop") {
        val initializer = declare(when {
            ctx.initD != null -> ctx.initD.accept(this)
            ctx.initE != null -> ctx.initE.accept(this)
            else -> null
        })
        LoopExpression(
                predicate = when (ctx.predicate) {
                    null -> ConstantExpression(1, ctx = ctx)
                    else -> ctx.predicate.accept(this).single()
                },
                body = ctx.statement().accept(this).single(),
                checkBefore = !ctx.getText().startsWith("do"),
                initializer = initializer,
                update = ctx.update?.let { it.accept(this) },
                ctx = ctx).let { listOf(it) }
    }

    override fun visitIfStatement(ctx: QCParser.IfStatementContext) = ConditionalExpression(
            test = ctx.expression().accept(this).single(),
            expression = false,
            pass = ctx.statement()[0].accept(this).single(),
            fail = when (ctx.statement().size()) {
                1 -> null
                else -> ctx.statement()[1]
            }?.accept(this)?.single(),
            ctx = ctx).let { listOf(it) }

    override fun visitSwitchStatement(ctx: QCParser.SwitchStatementContext) = SwitchExpression(
            test = ctx.expression().accept(this).single(),
            add = ctx.statement().accept(this),
            ctx = ctx).let { listOf(it) }

    override fun visitExpressionStatement(ctx: QCParser.ExpressionStatementContext) =
            ctx.expression()?.let { it.accept(this) } ?: Nop(ctx = ctx).let { listOf(it) }

    val QCParser.ExpressionContext.terminal: Boolean get() = expression() != null

    override fun visitExpression(ctx: QCParser.ExpressionContext) = if (ctx.terminal) {
        BinaryExpression.Comma(
                left = ctx.expression().accept(this).single(),
                right = ctx.assignmentExpression().accept(this).single(),
                ctx = ctx).let { listOf(it) }
    } else super.visitExpression(ctx)

    val QCParser.AssignmentExpressionContext.terminal: Boolean get() = assignmentExpression() != null

    override fun visitAssignmentExpression(ctx: QCParser.AssignmentExpressionContext) = when {
        ctx.terminal -> {
            val left = ctx.unaryExpression().accept(this).single()
            val right = ctx.assignmentExpression().accept(this).single()
            when (ctx.op.getType()) {
                QCParser.Assign -> BinaryExpression.Assign(left, right, ctx = ctx)
                QCParser.OrAssign -> BinaryExpression.OrAssign(left, right, ctx = ctx)
                QCParser.XorAssign -> BinaryExpression.ExclusiveOrAssign(left, right, ctx = ctx)
                QCParser.AndAssign -> BinaryExpression.AndAssign(left, right, ctx = ctx)
                QCParser.LeftShiftAssign -> BinaryExpression.LshAssign(left, right, ctx = ctx)
                QCParser.RightShiftAssign -> BinaryExpression.RshAssign(left, right, ctx = ctx)
                QCParser.PlusAssign -> BinaryExpression.AddAssign(left, right, ctx = ctx)
                QCParser.MinusAssign -> BinaryExpression.SubtractAssign(left, right, ctx = ctx)
                QCParser.StarAssign -> BinaryExpression.MultiplyAssign(left, right, ctx = ctx)
                QCParser.DivAssign -> BinaryExpression.DivideAssign(left, right, ctx = ctx)
                QCParser.ModAssign -> BinaryExpression.ModuloAssign(left, right, ctx = ctx)
                else -> throw NoWhenBranchMatchedException()
            }.let { listOf(it) }
        }
        else -> super.visitAssignmentExpression(ctx)
    }

    val QCParser.ConditionalExpressionContext.terminal: Boolean get() = expression().isNotEmpty()

    override fun visitConditionalExpression(ctx: QCParser.ConditionalExpressionContext) = when {
        ctx.terminal -> {
            ConditionalExpression(
                    test = ctx.logicalOrExpression().accept(this).single(),
                    expression = true,
                    pass = ctx.expression(0).accept(this).single(),
                    fail = ctx.expression(1).accept(this).single(),
                    ctx = ctx).let { listOf(it) }
        }
        else -> super.visitConditionalExpression(ctx)
    }

    val QCParser.LogicalOrExpressionContext.terminal: Boolean get() = logicalOrExpression() != null

    override fun visitLogicalOrExpression(ctx: QCParser.LogicalOrExpressionContext) = when {
        ctx.terminal -> {
            BinaryExpression.Or(
                    left = ctx.logicalOrExpression().accept(this).single(),
                    right = ctx.logicalAndExpression().accept(this).single(),
                    ctx = ctx).let { listOf(it) }
        }
        else -> super.visitLogicalOrExpression(ctx)
    }

    val QCParser.LogicalAndExpressionContext.terminal: Boolean get() = logicalAndExpression() != null

    override fun visitLogicalAndExpression(ctx: QCParser.LogicalAndExpressionContext) = when {
        ctx.terminal -> {
            BinaryExpression.And(
                    left = ctx.logicalAndExpression().accept(this).single(),
                    right = ctx.inclusiveOrExpression().accept(this).single(),
                    ctx = ctx).let { listOf(it) }
        }
        else -> super.visitLogicalAndExpression(ctx)
    }

    val QCParser.InclusiveOrExpressionContext.terminal: Boolean get() = inclusiveOrExpression() != null

    override fun visitInclusiveOrExpression(ctx: QCParser.InclusiveOrExpressionContext) = when {
        ctx.terminal -> {
            BinaryExpression.BitOr(
                    left = ctx.inclusiveOrExpression().accept(this).single(),
                    right = ctx.exclusiveOrExpression().accept(this).single(),
                    ctx = ctx).let { listOf(it) }
        }
        else -> super.visitInclusiveOrExpression(ctx)
    }

    val QCParser.ExclusiveOrExpressionContext.terminal: Boolean get() = exclusiveOrExpression() != null

    override fun visitExclusiveOrExpression(ctx: QCParser.ExclusiveOrExpressionContext) = when {
        ctx.terminal -> {
            BinaryExpression.ExclusiveOr(
                    left = ctx.exclusiveOrExpression().accept(this).single(),
                    right = ctx.andExpression().accept(this).single(),
                    ctx = ctx).let { listOf(it) }
        }
        else -> super.visitExclusiveOrExpression(ctx)
    }

    val QCParser.AndExpressionContext.terminal: Boolean get() = andExpression() != null

    override fun visitAndExpression(ctx: QCParser.AndExpressionContext) = when {
        ctx.terminal -> {
            BinaryExpression.BitAnd(
                    left = ctx.andExpression().accept(this).single(),
                    right = ctx.equalityExpression().accept(this).single(),
                    ctx = ctx).let { listOf(it) }
        }
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
            // TODO
            BinaryExpression.Multiply(left, right, ctx = ctx).let { listOf(it) }
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
                else -> throw NoWhenBranchMatchedException()
            }.let { listOf(it) }
        }
        else -> super.visitMultiplicativeExpression(ctx)
    }

    val QCParser.CastExpressionContext.terminal: Boolean get() = castExpression() != null

    override fun visitCastExpression(ctx: QCParser.CastExpressionContext) = when {
        ctx.terminal -> {
            UnaryExpression.Cast(
                    type = state.types[ctx.typeName().getText()],
                    operand = ctx.castExpression().accept(this).single(),
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

    override fun visitPostfixCall(ctx: QCParser.PostfixCallContext) = MethodCallExpression(
            function = ctx.postfixExpression().accept(this).single(),
            add = ctx.argumentExpressionList()?.assignmentExpression()?.let {
                it.flatMap { it.accept(this) }.filterNotNull()
            } ?: emptyList(),
            ctx = ctx).let { listOf(it) }

    val matchVecComponent = "(.+)_(x|y|z)$".toRegex()

    /**
     * static:
     * struct.field
     */
    override fun visitPostfixField(ctx: QCParser.PostfixFieldContext): List<Expression> {
        val left = ctx.postfixExpression().accept(this).single()
        val text = ctx.Identifier().getText()
        val legacyVectors = true
        val matcher = matchVecComponent.matcher(text)
        return when {
            legacyVectors && matcher.matches() -> {
                // `ent.vec_x` -> `ent.vec.x`
                // hides similarly named fields, but so be it
                val vector = matcher.group(1)
                val component = matcher.group(2)
                MemberExpression(
                        left = MemberExpression(
                                left = left,
                                field = vector),
                        field = component,
                        ctx = ctx)
            }
            else -> MemberExpression(left = left, field = text, ctx = ctx)
        }.let { listOf(it) }
    }

    /**
     * dynamic:
     * entity.(field)
     */
    override fun visitPostfixAddress(ctx: QCParser.PostfixAddressContext) = IndexExpression(
            left = ctx.postfixExpression().accept(this).single(),
            right = ctx.expression().accept(this).single(),
            ctx = ctx).let { listOf(it) }

    /**
     * dynamic:
     * array[index]
     */
    override fun visitPostfixIndex(ctx: QCParser.PostfixIndexContext) = IndexExpression(
            left = ctx.postfixExpression().accept(this).single(),
            right = ctx.expression().accept(this).single(),
            ctx = ctx).let { listOf(it) }

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

    [suppress("NOTHING_TO_INLINE")]
    inline fun String.unquote() = substring(1, length() - 1)

    override fun visitPrimaryExpression(ctx: QCParser.PrimaryExpressionContext): List<Expression> {
        ctx.expression()?.let { return it.accept(this) }
        val text = ctx.getText()
        ctx.Identifier()?.let {
            return ReferenceExpression(resolve(text)!!, ctx = ctx).let { listOf(it) }
        }
        val str = ctx.StringLiteral()
        if (str.isNotEmpty()) {
            return ConstantExpression(StringBuilder { str.forEach { append(it.getText().unquote()) } }.toString(),
                    ctx = ctx).let { listOf(it) }
        }
        if (text.startsWith('#')) {
            return ConstantExpression(text, ctx = ctx).let { listOf(it) }
        }
        ctx.Constant()?.let {
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
            val f = s.toFloat()
            val i = f.toInt()
            return (ConstantExpression(when (i.toFloat()) {
                f -> i
                else -> f
            }, ctx = ctx)).let { listOf(it) }
        }
        return UnaryExpression.Cast(void_t, DynamicReferenceExpression("FIXME_${text}", ctx = ctx)).let { listOf(it) }
    }
}
