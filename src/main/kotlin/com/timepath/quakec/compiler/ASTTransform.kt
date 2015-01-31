package com.timepath.quakec.compiler

import java.util.regex.Pattern
import com.timepath.quakec.Logging
import com.timepath.quakec.QCBaseVisitor
import com.timepath.quakec.QCParser
import com.timepath.quakec.QCParser.DeclarationSpecifierContext
import com.timepath.quakec.QCParser.ParameterTypeListContext
import com.timepath.quakec.compiler.ast.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode

class ASTTransform(val types: TypeRegistry) : QCBaseVisitor<List<Expression>>() {

    class object {
        val logger = Logging.new()
    }

    private fun debug(ctx: ParserRuleContext) {
        val token = ctx.start
        val source = token.getTokenSource()

        val line = token.getLine()
        val col = token.getCharPositionInLine()
        val file = source.getSourceName()
        logger.fine("{$token} $line,$col $file")
    }

    fun QCParser.DeclarationSpecifiersContext.type(old: Boolean = false): Type? {
        [suppress("SENSELESS_COMPARISON")]
        if (this == null) return null
        return type(this.declarationSpecifier(), old)
    }

    fun QCParser.DeclarationSpecifiers2Context.type(old: Boolean = false): Type? {
        [suppress("SENSELESS_COMPARISON")]
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
            typeSpec.pointer()?.getText()?.length() == 3 -> Type.Void
            else -> types[typeSpec.directTypeSpecifier().children[0].getText()]
        }
        var indirect = if (functional != null && !old) {
            functional.functionType(direct)
        } else {
            direct
        }
        indirection.times {
            indirect = Type.Field(indirect)
        }
        return indirect
    }

    fun ParameterTypeListContext.functionType(type: Type): Type {
        [suppress("SENSELESS_COMPARISON")]
        if (this == null) return type
        val parameterList = this.parameterList()
        val parameterVarargs = this.parameterVarargs()
        val parameterDeclarations = parameterList?.parameterDeclaration()
        val typeArgs = parameterDeclarations?.map { it.declarationSpecifiers()?.type() ?: it.declarationSpecifiers2()?.type() } ?: emptyList()
        val vararg = parameterVarargs?.declarationSpecifiers()?.type()
        val function = Type.Function(type, typeArgs.requireNoNulls(), vararg)
        return function
    }

    fun ParameterTypeListContext.functionArgs(ctx: ParserRuleContext): List<Expression> {
        [suppress("UNNECESSARY_SAFE_CALL")]
        val paramDeclarations = this?.parameterList()?.parameterDeclaration() ?: emptyList()
        val params = paramDeclarations.mapIndexed {(i, it) ->
            val paramId = it.declarator()?.getText()
            if (paramId != null) {
                val type = it.declarationSpecifiers().type()
                val param = ParameterExpression(paramId, type!!, index = i, ctx = ctx)
                listOf(param)
            } else {
                emptyList()
            }
        }.flatMap { it }
        return params
    }

    fun ParameterTypeListContext.functionVararg(ctx: ParserRuleContext): Expression? {
        [suppress("UNNECESSARY_SAFE_CALL")]
        val varargs = this?.parameterVarargs()
        val vararg = when (varargs) {
            null -> null
            else -> {
                val type = varargs.declarationSpecifiers().type() ?: Type.Void
                DeclarationExpression(varargs.Identifier()?.getText() ?: "...", type, null, ctx = ctx)
            }
        }
        return vararg
    }

    override fun defaultResult(): List<Expression> = emptyList()

    override fun aggregateResult(aggregate: List<Expression>, nextResult: List<Expression>): List<Expression> {
        return aggregate + nextResult
    }

    override fun visitCompilationUnit(ctx: QCParser.CompilationUnitContext): List<Expression> {
        return listOf(BlockExpression(visitChildren(ctx), ctx = ctx))
    }

    override fun visitCompoundStatement(ctx: QCParser.CompoundStatementContext): List<Expression> {
        return listOf(BlockExpression(visitChildren(ctx), ctx = ctx))
    }

    override fun visitFunctionDefinition(ctx: QCParser.FunctionDefinitionContext): List<Expression> {
        var declarator = ctx.declarator()
        while (declarator.declarator() != null) {
            declarator = declarator.declarator()
        }
        val id = declarator.getText()

        val old = ctx.declarator().parameterTypeList() == null
        val parameterTypeList = if (old) {
            ctx.declarationSpecifiers().declarationSpecifier().last().typeSpecifier().directTypeSpecifier().parameterTypeList()
        } else {
            ctx.declarator().parameterTypeList()
        }
        val retType = ctx.declarationSpecifiers().type(old)
        val params = parameterTypeList.functionArgs(ctx)
        val vararg = parameterTypeList.functionVararg(ctx)
        val signature = parameterTypeList.functionType(retType!!);
        return listOf(FunctionExpression(id, signature as Type.Function, params, vararg, visitChildren(ctx.compoundStatement()), ctx = ctx))
    }

    override fun visitDeclaration(ctx: QCParser.DeclarationContext): List<Expression> {
        val declarations = ctx.initDeclaratorList()?.initDeclarator()
        if (declarations == null) {
            val enum = ctx.enumSpecifier()
            return enum.enumeratorList().enumerator().map {
                val id = it.enumerationConstant().getText()
                Type.Float.declare(id).single()
            }
        }
        val typedef = ctx.declarationSpecifiers().declarationSpecifier().firstOrNull { it.storageClassSpecifier()?.getText() == "typedef" } != null
        if (typedef) {
            val type = type(ctx.declarationSpecifiers().declarationSpecifier())
            ctx.initDeclaratorList().initDeclarator().forEach {
                types[it.getText()] = type!!
            }
            return emptyList()
        }
        val type = ctx.declarationSpecifiers().type()
        return declarations.flatMap {
            var declarator = it.declarator()
            while (declarator.declarator() != null) {
                declarator = declarator.declarator()
            }
            val arraySize = it.declarator().assignmentExpression()
            val id = declarator.getText()
            val initializer = it.initializer()?.accept(this)?.single()
            when (initializer) {
                is ConstantExpression -> {
                    val value = initializer.evaluate()
                    val s = value.value.toString()
                    if (s.startsWith('#')) {
                        // FIXME: HACK
                        val i = s.substring(1).toInt()
                        // Similar to function definition
                        val old = it.declarator().parameterTypeList() == null
                        val parameterTypeList = if (old) {
                            ctx.declarationSpecifiers().declarationSpecifier().last().typeSpecifier().directTypeSpecifier().parameterTypeList()
                        } else {
                            it.declarator().parameterTypeList()
                        }
                        val retType = ctx.declarationSpecifiers().type(old)
                        val params = parameterTypeList.functionArgs(ctx)
                        val vararg = parameterTypeList.functionVararg(ctx)
                        val signature = parameterTypeList.functionType(retType!!);
                        listOf(FunctionExpression(id, signature as Type.Function, params = params, vararg = vararg, builtin = i, ctx = ctx))
                    } else {
                        type!!.declare(id, initializer)
                    }
                }
                is Expression -> {
                    type!!.declare(id).flatMap {
                        listOf(it, BinaryExpression.Assign(it, initializer, ctx = ctx))
                    }
                }
                else -> {
                    val parameterTypeList = it.declarator().parameterTypeList()
                    if (arraySize != null) {
                        val sizeExpr = arraySize.accept(this).single()
                        Type.Array(type!!, sizeExpr).declare(id)
                    } else {
                        parameterTypeList.functionType(type!!).declare(id)
                    }
                }
            }
        }
    }

    override fun visitCustomLabel(ctx: QCParser.CustomLabelContext): List<Expression> {
        with(linkedListOf<Expression>()) {
            add(LabelExpression(ctx.Identifier().getText(), ctx = ctx))
            addAll(ctx.blockItem()?.accept(this@ASTTransform) ?: emptyList())
            return this
        }
    }

    override fun visitCaseLabel(ctx: QCParser.CaseLabelContext): List<Expression> {
        with(linkedListOf<Expression>()) {
            add(SwitchExpression.Case(ctx.constantExpression().accept(this@ASTTransform).single(), ctx = ctx))
            addAll(ctx.blockItem()?.accept(this@ASTTransform) ?: emptyList())
            return this
        }
    }

    override fun visitDefaultLabel(ctx: QCParser.DefaultLabelContext): List<Expression> {
        with(linkedListOf<Expression>()) {
            add(SwitchExpression.Case(null, ctx = ctx))
            addAll(ctx.blockItem()?.accept(this@ASTTransform) ?: emptyList())
            return this
        }
    }

    override fun visitReturnStatement(ctx: QCParser.ReturnStatementContext): List<Expression> {
        val expr = ctx.expression()
        val retVal = expr?.accept(this)?.single()
        return listOf(ReturnStatement(when {
            retVal is Expression -> retVal : Expression
            else -> null
        }, ctx = ctx))
    }

    override fun visitBreakStatement(ctx: QCParser.BreakStatementContext): List<Expression> {
        return listOf(BreakStatement(ctx = ctx))
    }

    override fun visitContinueStatement(ctx: QCParser.ContinueStatementContext): List<Expression> {
        return listOf(ContinueStatement(ctx = ctx))
    }

    override fun visitGotoStatement(ctx: QCParser.GotoStatementContext): List<Expression> {
        return listOf(GotoExpression(ctx.Identifier().getText(), ctx = ctx))
    }

    override fun visitIterationStatement(ctx: QCParser.IterationStatementContext): List<Expression> {
        val predicate = ctx.predicate
        val predicateExpr = when (predicate) {
            null -> ConstantExpression(1, ctx = ctx)
            else -> predicate.accept(this).single()
        }

        val bodyStmt = ctx.statement().accept(this).single()
        val checkBefore = !ctx.getText().startsWith("do")
        val initializer = when {
            ctx.initD != null -> ctx.initD.accept(this)
            ctx.initE != null -> ctx.initE.accept(this)
            else -> null
        }
        val update = when (ctx.update) {
            null -> null
            else -> ctx.update.accept(this)
        }
        return listOf(LoopExpression(predicateExpr, bodyStmt, checkBefore, initializer, update, ctx = ctx))
    }

    override fun visitIfStatement(ctx: QCParser.IfStatementContext): List<Expression> {
        val get = {(i: Int) -> ctx.statement()[i] }
        val test = ctx.expression()
        val yes = get(0)
        val no = when (ctx.statement().size()) {
            1 -> null
            else -> get(1)
        }
        val testExpr = test.accept(this).single()
        val yesExpr = yes.accept(this).single()
        val noExpr = no?.accept(this)?.single()
        return listOf(ConditionalExpression(testExpr, false, yesExpr, noExpr, ctx = ctx))
    }

    override fun visitSwitchStatement(ctx: QCParser.SwitchStatementContext): List<Expression> {
        val testExpr = ctx.expression().accept(this).single()
        val switch = SwitchExpression(testExpr, ctx.statement().accept(this), ctx = ctx)
        return listOf(switch)
    }

    override fun visitExpressionStatement(ctx: QCParser.ExpressionStatementContext): List<Expression> {
        val expr = ctx.expression()
        if (expr != null) {
            return expr.accept(this)
        }
        // redundant semicolon
        return listOf(Nop(ctx = ctx))
    }

    val QCParser.ExpressionContext.terminal: Boolean get() = expression() != null

    override fun visitExpression(ctx: QCParser.ExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.expression().accept(this).single()
            val right = ctx.assignmentExpression().accept(this).single()
            return listOf(BinaryExpression.Comma(left, right, ctx = ctx))
        }
        return super.visitExpression(ctx)
    }

    val QCParser.AssignmentExpressionContext.terminal: Boolean get() = assignmentExpression() != null

    override fun visitAssignmentExpression(ctx: QCParser.AssignmentExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.unaryExpression().accept(this).single()
            val right = ctx.assignmentExpression().accept(this).single()
            val ref = left
            val value = right
            val op = when (ctx.op.getType()) {
                QCParser.Assign -> BinaryExpression.Assign(ref, value, ctx = ctx)
                QCParser.OrAssign -> BinaryExpression.OrAssign(ref, value, ctx = ctx)
                QCParser.XorAssign -> BinaryExpression.ExclusiveOrAssign(ref, value, ctx = ctx)
                QCParser.AndAssign -> BinaryExpression.AndAssign(ref, value, ctx = ctx)
                QCParser.LeftShiftAssign -> BinaryExpression.LshAssign(ref, value, ctx = ctx)
                QCParser.RightShiftAssign -> BinaryExpression.RshAssign(ref, value, ctx = ctx)
                QCParser.PlusAssign -> BinaryExpression.AddAssign(ref, value, ctx = ctx)
                QCParser.MinusAssign -> BinaryExpression.SubtractAssign(ref, value, ctx = ctx)
                QCParser.StarAssign -> BinaryExpression.MultiplyAssign(ref, value, ctx = ctx)
                QCParser.DivAssign -> BinaryExpression.DivideAssign(ref, value, ctx = ctx)
                QCParser.ModAssign -> BinaryExpression.ModuloAssign(ref, value, ctx = ctx)
                else -> null
            }
            if (op != null) return listOf(op)
            return emptyList()
        }
        return super.visitAssignmentExpression(ctx)
    }

    val QCParser.ConditionalExpressionContext.terminal: Boolean get() = expression().isNotEmpty()

    override fun visitConditionalExpression(ctx: QCParser.ConditionalExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val condition = ctx.logicalOrExpression().accept(this).single()
            val yes = ctx.expression(0).accept(this).single()
            val no = ctx.expression(1).accept(this).single()
            return listOf(ConditionalExpression(condition, true, yes, no, ctx = ctx))
        }
        return super.visitConditionalExpression(ctx)
    }

    val QCParser.LogicalOrExpressionContext.terminal: Boolean get() = logicalOrExpression() != null

    override fun visitLogicalOrExpression(ctx: QCParser.LogicalOrExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.logicalOrExpression().accept(this).single()
            val right = ctx.logicalAndExpression().accept(this).single()
            return listOf(BinaryExpression.Or(left, right, ctx = ctx))
        }
        return super.visitLogicalOrExpression(ctx)
    }

    val QCParser.LogicalAndExpressionContext.terminal: Boolean get() = logicalAndExpression() != null

    override fun visitLogicalAndExpression(ctx: QCParser.LogicalAndExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.logicalAndExpression().accept(this).single()
            val right = ctx.inclusiveOrExpression().accept(this).single()
            return listOf(BinaryExpression.And(left, right, ctx = ctx))
        }
        return super.visitLogicalAndExpression(ctx)
    }

    val QCParser.InclusiveOrExpressionContext.terminal: Boolean get() = inclusiveOrExpression() != null

    override fun visitInclusiveOrExpression(ctx: QCParser.InclusiveOrExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.inclusiveOrExpression().accept(this).single()
            val right = ctx.exclusiveOrExpression().accept(this).single()
            return listOf(BinaryExpression.BitOr(left, right, ctx = ctx))
        }
        return super.visitInclusiveOrExpression(ctx)
    }

    val QCParser.ExclusiveOrExpressionContext.terminal: Boolean get() = exclusiveOrExpression() != null

    override fun visitExclusiveOrExpression(ctx: QCParser.ExclusiveOrExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.exclusiveOrExpression().accept(this).single()
            val right = ctx.andExpression().accept(this).single()
            return listOf(BinaryExpression.ExclusiveOr(left, right, ctx = ctx))
        }
        return super.visitExclusiveOrExpression(ctx)
    }

    val QCParser.AndExpressionContext.terminal: Boolean get() = andExpression() != null

    override fun visitAndExpression(ctx: QCParser.AndExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.andExpression().accept(this).single()
            val right = ctx.equalityExpression().accept(this).single()
            return listOf(BinaryExpression.BitAnd(left, right, ctx = ctx))
        }
        return super.visitAndExpression(ctx)
    }

    val QCParser.EqualityExpressionContext.terminal: Boolean get() = equalityExpression() != null

    override fun visitEqualityExpression(ctx: QCParser.EqualityExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.equalityExpression().accept(this).single()
            val right = ctx.relationalExpression().accept(this).single()
            val op = when (ctx.op.getType()) {
                QCParser.Equal -> BinaryExpression.Eq(left, right, ctx = ctx)
                QCParser.NotEqual -> BinaryExpression.Ne(left, right, ctx = ctx)
                else -> null
            }
            if (op != null) {
                return listOf(op)
            } else {
                return emptyList()
            }
        }
        return super.visitEqualityExpression(ctx)
    }

    val QCParser.RelationalExpressionContext.terminal: Boolean get() = relationalExpression() != null

    override fun visitRelationalExpression(ctx: QCParser.RelationalExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.relationalExpression().accept(this).single()
            val right = ctx.shiftExpression().accept(this).single()
            val op = when (ctx.op.getType()) {
                QCParser.Less -> BinaryExpression.Lt(left, right, ctx = ctx)
                QCParser.LessEqual -> BinaryExpression.Le(left, right, ctx = ctx)
                QCParser.Greater -> BinaryExpression.Gt(left, right, ctx = ctx)
                QCParser.GreaterEqual -> BinaryExpression.Ge(left, right, ctx = ctx)
                else -> null
            }
            if (op != null) {
                return listOf(op)
            } else {
                return emptyList()
            }
        }
        return super.visitRelationalExpression(ctx)
    }

    val QCParser.ShiftExpressionContext.terminal: Boolean get() = shiftExpression() != null

    override fun visitShiftExpression(ctx: QCParser.ShiftExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.shiftExpression().accept(this).single()
            val right = ctx.additiveExpression().accept(this).single()
            // TODO
            return listOf(BinaryExpression.Multiply(left, right, ctx = ctx))
        }
        return super.visitShiftExpression(ctx)
    }

    val QCParser.AdditiveExpressionContext.terminal: Boolean get() = additiveExpression() != null

    override fun visitAdditiveExpression(ctx: QCParser.AdditiveExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.additiveExpression().accept(this).single()
            val right = ctx.multiplicativeExpression().accept(this).single()
            val op = when (ctx.op.getType()) {
                QCParser.Plus -> BinaryExpression.Add(left, right, ctx = ctx)
                QCParser.Minus -> BinaryExpression.Subtract(left, right, ctx = ctx)
                else -> null
            }
            return if (op != null) listOf(op) else emptyList()
        }
        return super.visitAdditiveExpression(ctx)
    }

    val QCParser.MultiplicativeExpressionContext.terminal: Boolean get() = multiplicativeExpression() != null

    override fun visitMultiplicativeExpression(ctx: QCParser.MultiplicativeExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.multiplicativeExpression().accept(this).single()
            val right = ctx.castExpression().accept(this).single()
            val op = when (ctx.op.getType()) {
                QCParser.Star -> BinaryExpression.Multiply(left, right, ctx = ctx)
                QCParser.Div -> BinaryExpression.Divide(left, right, ctx = ctx)
                QCParser.Mod -> BinaryExpression.Modulo(left, right, ctx = ctx)
                else -> null
            }
            return if (op != null) listOf(op) else emptyList()
        }
        return super.visitMultiplicativeExpression(ctx)
    }

    val QCParser.CastExpressionContext.terminal: Boolean get() = castExpression() != null

    override fun visitCastExpression(ctx: QCParser.CastExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val left = ctx.castExpression().accept(this).single()
            // TODO
            //            val right = ctx.typeName().accept(this).single()
            //            return BinaryExpression.Cast(left, right)
            return listOf(left)
        }
        return super.visitCastExpression(ctx)
    }

    val QCParser.UnaryExpressionContext.terminal: Boolean get() = unaryExpression() != null

    override fun visitUnaryExpression(ctx: QCParser.UnaryExpressionContext): List<Expression> {
        if (ctx.terminal) {
            val right = ctx.unaryExpression().accept(this).single()
            val expr = right
            val expand = when (ctx.op.getType()) {
                QCParser.PlusPlus -> UnaryExpression.PreIncrement(expr, ctx = ctx)
                QCParser.MinusMinus -> UnaryExpression.PreDecrement(expr, ctx = ctx)
                QCParser.And -> UnaryExpression.Address(expr, ctx = ctx)
                QCParser.Star -> UnaryExpression.Dereference(expr, ctx = ctx)
                QCParser.Plus -> UnaryExpression.Plus(expr, ctx = ctx)
                QCParser.Minus -> UnaryExpression.Minus(expr, ctx = ctx)
                QCParser.Tilde -> UnaryExpression.BitNot(expr, ctx = ctx)
                QCParser.Not -> UnaryExpression.Not(expr, ctx = ctx)
                else -> right
            }
            return listOf(expand)
        }
        val typeName = ctx.typeName()
        if (typeName != null) {
            return listOf(ReferenceExpression("TODO: sizeof(${typeName.getText()})", ctx = ctx))
        }
        return super.visitUnaryExpression(ctx)
    }

    override fun visitPostfixPrimary(ctx: QCParser.PostfixPrimaryContext): List<Expression> {
        val left = ctx.primaryExpression().accept(this).single()
        return listOf(left)
    }

    override fun visitPostfixCall(ctx: QCParser.PostfixCallContext): List<Expression> {
        val left = ctx.postfixExpression().accept(this).single()

        val right = ctx.argumentExpressionList()
                ?.assignmentExpression()
                ?.flatMap { it.accept(this) }
                ?.filterNotNull()
        return listOf(MethodCallExpression(left, right ?: emptyList(), ctx = ctx))
    }

    val vecRegex = Pattern.compile("(.+)_([xyz])$")

    override fun visitPostfixField(ctx: QCParser.PostfixFieldContext): List<Expression> {
        val left = ctx.postfixExpression().accept(this).single()
        val right = ctx.Identifier()
        val text = right.getText()
        val matcher = vecRegex.matcher(text)
        return listOf(when {
            matcher.matches() -> {
                val fieldRef = EntityFieldReference(matcher.group(1))
                val l = MemberExpression(left, fieldRef)
                MemberExpression(l, ReferenceExpression(matcher.group(2)), ctx = ctx)
            }
            else -> {
                val fieldRef = EntityFieldReference(text)
                MemberExpression(left, fieldRef, ctx = ctx)
            }
        })

    }

    override fun visitPostfixAddress(ctx: QCParser.PostfixAddressContext): List<Expression> {
        val left = ctx.postfixExpression().accept(this).single()
        val right = ctx.expression().accept(this).single()
        // TODO
        return listOf(MemberExpression(left, right, ctx = ctx))
    }

    override fun visitPostfixIndex(ctx: QCParser.PostfixIndexContext): List<Expression> {
        val left = ctx.postfixExpression().accept(this).single()
        val right = ctx.expression().accept(this).single()
        // TODO
        return listOf(MemberExpression(left, right, ctx = ctx))
    }

    override fun visitPostfixIncr(ctx: QCParser.PostfixIncrContext): List<Expression> {
        val expr = ctx.postfixExpression().accept(this).single()
        val expand = when (ctx.op.getType()) {
            QCParser.PlusPlus -> UnaryExpression.PostIncrement(expr, ctx = ctx)
            QCParser.MinusMinus -> UnaryExpression.PostDecrement(expr, ctx = ctx)
            else -> expr
        }
        return listOf(expand)
    }

    val matchChar = Pattern.compile("'(.)'")
    val matchVec = Pattern.compile("'\\s*([+-]?[\\d.]+)\\s*([+-]?[\\d.]+)\\s*([+-]?[\\d.]+)\\s*'")
    val matchHex = Pattern.compile("0x([\\d0-F]+)")

    override fun visitPrimaryExpression(ctx: QCParser.PrimaryExpressionContext): List<Expression> {
        val expressionContext = ctx.expression()
        if (expressionContext != null) {
            return expressionContext.accept(this)
        }
        val text = ctx.getText()
        if (ctx.Identifier() != null) {
            return listOf(ReferenceExpression(text, ctx = ctx))
        }
        if (ctx.StringLiteral().isNotEmpty()) {
            val concat = ctx.StringLiteral()
                    .fold("", {(ret: String, string: TerminalNode) ->
                        val s = string.getText()
                        ret + s.substring(1, s.length() - 1)
                    })
            return listOf(ConstantExpression(concat, ctx = ctx))
        }
        if (text.startsWith('#')) {
            return listOf(ConstantExpression(text, ctx = ctx))
        }
        if (ctx.Constant() != null) {
            val constant = ctx.Constant()
            val s = constant.getText()
            matchChar.let {
                val matcher = it.matcher(s)
                if (matcher.matches()) {
                    val c1 = matcher.group(1)
                    return listOf(ConstantExpression(c1.charAt(0), ctx = ctx))
                }
            }
            matchVec.let {
                val matcher = it.matcher(s)
                if (matcher.matches()) {
                    val c1 = matcher.group(1).toFloat()
                    val c2 = matcher.group(2).toFloat()
                    val c3 = matcher.group(3).toFloat()
                    return listOf(ConstantExpression(array(c1, c2, c3), ctx = ctx))
                }
            }
            matchHex.let {
                val matcher = it.matcher(s)
                if (matcher.matches()) {
                    val hex = Integer.parseInt(matcher.group(1), 16)
                    return listOf(ConstantExpression(hex.toFloat(), ctx = ctx))
                }
            }
            val f = s.toFloat()
            return listOf(ConstantExpression(f, ctx = ctx))
        }
        return listOf(ConstantExpression("FIXME_${text}", ctx = ctx))
    }
}
