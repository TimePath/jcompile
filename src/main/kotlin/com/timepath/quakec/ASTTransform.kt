package com.timepath.quakec

import com.timepath.quakec.ast.Statement
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.ast.BlockStatement
import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.Type
import com.timepath.quakec.ast.impl.FunctionLiteral
import com.timepath.quakec.ast.impl.DeclarationExpression
import com.timepath.quakec.ast.impl.ReturnStatement
import com.timepath.quakec.ast.impl.ReferenceExpression
import com.timepath.quakec.ast.impl.BinaryExpression
import com.timepath.quakec.ast.impl.ConditionalExpression
import com.timepath.quakec.ast.impl.ConstantExpression
import com.timepath.quakec.ast.impl.FunctionCall
import org.antlr.v4.runtime.tree.TerminalNode

class ASTTransform : QCBaseVisitor<List<Statement>>() {

    private fun debug(ctx: ParserRuleContext) {
        val token = ctx.start
        val source = token.getTokenSource()

        val line = token.getLine()
        val col = token.getCharPositionInLine()
        val file = source.getSourceName()
        println("I: {$token} $line,$col $file")
    }

    override fun defaultResult(): List<Statement> = emptyList()

    override fun aggregateResult(aggregate: List<Statement>, nextResult: List<Statement>): List<Statement> {
        return aggregate + nextResult
    }

    override fun visitCompilationUnit(ctx: QCParser.CompilationUnitContext): List<Statement> {
        return listOf(BlockStatement(visitChildren(ctx)))
    }

    override fun visitCompoundStatement(ctx: QCParser.CompoundStatementContext): List<Statement> {
        return listOf(BlockStatement(visitChildren(ctx)))
    }

    override fun visitFunctionDefinition(ctx: QCParser.FunctionDefinitionContext): List<Statement> {
        var declarator = ctx.declarator()
        while (declarator.declarator() != null) {
            declarator = declarator.declarator()
        }
        val id = declarator.getText()

        val old = ctx.declarator().parameterTypeList() == null
        val paramContext = if (old) {
            ctx.declarationSpecifiers().declarationSpecifier().map {
                it.typeSpecifier().directTypeSpecifier().parameterTypeList()
            }
        } else {
            listOf(ctx.declarator().parameterTypeList())
        }
        val paramDeclarations = paramContext.flatMap {
            it.parameterList().parameterDeclaration()
        }
        val params = paramDeclarations.map {
            val paramId = it.declarator()?.getText()
            if (paramId != null) DeclarationExpression(paramId) else null
        }.filterNotNull()
        return listOf(FunctionLiteral(id, Type.Void, array(), params + visitChildren(ctx.compoundStatement())))
    }

    override fun visitDeclaration(ctx: QCParser.DeclarationContext): List<Statement> {
        val declarations = ctx.initDeclaratorList().initDeclarator()
        return declarations.map {
            var declarator = it.declarator()
            while (declarator.declarator() != null) {
                declarator = declarator.declarator()
            }
            val id = declarator.getText()
            DeclarationExpression(id)
        }
    }

    override fun visitJumpStatement(ctx: QCParser.JumpStatementContext): List<Statement> {
        // TODO: break, continue
        val expr = ctx.expression()
        val ret = if (expr != null) {
            val list = expr.accept(this)[0]
            ReturnStatement(list as Expression)
        } else {
            ReturnStatement(null)
        }
        return listOf(ret)
    }

    override fun visitIterationStatement(ctx: QCParser.IterationStatementContext): List<Statement> {
        // TODO
        return listOf(ConstantExpression("TODO: Iteration"))
    }

    override fun visitIfStatement(ctx: QCParser.IfStatementContext): List<Statement> {
        val get = {(i: Int) -> ctx.statement()[i] }
        val test = ctx.expression()
        val yes = get(0)
        val no = when (ctx.statement().size()) {
            1 -> null
            else -> get(1)
        }
        val testExpr = test.accept(this)[0]
        val yesExpr = yes.accept(this)[0]
        val noExpr = no?.accept(this)?.get(0)
        return listOf(ConditionalExpression(testExpr as Expression, yesExpr, noExpr))
    }

    override fun visitExpressionStatement(ctx: QCParser.ExpressionStatementContext): List<Statement> {
        val expr = ctx.expression()
        if (expr != null) {
            return expr.accept(this)
        }
        // redundant semicolon
        return listOf()
    }

    val QCParser.AssignmentExpressionContext.terminal: Boolean get() = assignmentExpression() != null

    override fun visitAssignmentExpression(ctx: QCParser.AssignmentExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.unaryExpression().accept(this)[0]
            val right = ctx.assignmentExpression().accept(this)[0]
            val ref = left as ReferenceExpression
            val value = right as Expression
            val assign = {(value: Expression): Expression ->
                BinaryExpression.Assign(ref, value)
            }
            val op = when (ctx.op.getType()) {
                QCParser.Assign -> assign(value)
                QCParser.OrAssign -> assign(BinaryExpression.BitOr(ref, value))
                QCParser.XorAssign -> assign(BinaryExpression.BitXor(ref, value))
                QCParser.AndAssign -> assign(BinaryExpression.BitAnd(ref, value))
                QCParser.LeftShiftAssign -> assign(BinaryExpression.Lsh(ref, value))
                QCParser.RightShiftAssign -> assign(BinaryExpression.Rsh(ref, value))
                QCParser.PlusAssign -> assign(BinaryExpression.Add(ref, value))
                QCParser.MinusAssign -> assign(BinaryExpression.Sub(ref, value))
                QCParser.StarAssign -> assign(BinaryExpression.Mul(ref, value))
                QCParser.DivAssign -> assign(BinaryExpression.Div(ref, value))
                QCParser.ModAssign -> assign(BinaryExpression.Mod(ref, value))
                else -> null
            }
            if (op != null) return listOf(op)
            return listOf()
        }
        return super.visitAssignmentExpression(ctx)
    }

    val QCParser.ConditionalExpressionContext.terminal: Boolean get() = expression().isNotEmpty()

    override fun visitConditionalExpression(ctx: QCParser.ConditionalExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val condition = ctx.logicalOrExpression().accept(this)[0]
            val yes = ctx.expression(0).accept(this)[0]
            val no = ctx.expression(1).accept(this)[0]
            return listOf(ConditionalExpression(condition as Expression, yes, no))
        }
        return super.visitConditionalExpression(ctx)
    }

    val QCParser.LogicalOrExpressionContext.terminal: Boolean get() = logicalOrExpression() != null

    override fun visitLogicalOrExpression(ctx: QCParser.LogicalOrExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.logicalOrExpression().accept(this)[0]
            val right = ctx.logicalAndExpression().accept(this)[0]
            return listOf(BinaryExpression.Or(left as Expression, right as Expression))
        }
        return super.visitLogicalOrExpression(ctx)
    }

    val QCParser.LogicalAndExpressionContext.terminal: Boolean get() = logicalAndExpression() != null

    override fun visitLogicalAndExpression(ctx: QCParser.LogicalAndExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.logicalAndExpression().accept(this)[0]
            val right = ctx.inclusiveOrExpression().accept(this)[0]
            return listOf(BinaryExpression.And(left as Expression, right as Expression))
        }
        return super.visitLogicalAndExpression(ctx)
    }

    val QCParser.InclusiveOrExpressionContext.terminal: Boolean get() = inclusiveOrExpression() != null

    override fun visitInclusiveOrExpression(ctx: QCParser.InclusiveOrExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.inclusiveOrExpression().accept(this)[0]
            val right = ctx.exclusiveOrExpression().accept(this)[0]
            return listOf(BinaryExpression.BitOr(left as Expression, right as Expression))
        }
        return super.visitInclusiveOrExpression(ctx)
    }

    val QCParser.ExclusiveOrExpressionContext.terminal: Boolean get() = exclusiveOrExpression() != null

    override fun visitExclusiveOrExpression(ctx: QCParser.ExclusiveOrExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.exclusiveOrExpression().accept(this)[0]
            val right = ctx.andExpression().accept(this)[0]
            return listOf(BinaryExpression.BitXor(left as Expression, right as Expression))
        }
        return super.visitExclusiveOrExpression(ctx)
    }

    val QCParser.AndExpressionContext.terminal: Boolean get() = andExpression() != null

    override fun visitAndExpression(ctx: QCParser.AndExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.andExpression().accept(this)[0]
            val right = ctx.equalityExpression().accept(this)[0]
            return listOf(BinaryExpression.BitAnd(left as Expression, right as Expression))
        }
        return super.visitAndExpression(ctx)
    }

    val QCParser.EqualityExpressionContext.terminal: Boolean get() = equalityExpression() != null

    override fun visitEqualityExpression(ctx: QCParser.EqualityExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.equalityExpression().accept(this)[0]
            val right = ctx.relationalExpression().accept(this)[0]
            // TODO
            return listOf(BinaryExpression.Eq(left as Expression, right as Expression))
        }
        return super.visitEqualityExpression(ctx)
    }

    val QCParser.RelationalExpressionContext.terminal: Boolean get() = relationalExpression() != null

    override fun visitRelationalExpression(ctx: QCParser.RelationalExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.relationalExpression().accept(this)[0]
            val right = ctx.shiftExpression().accept(this)[0]
            // TODO
            return listOf(BinaryExpression.Le(left as Expression, right as Expression))
        }
        return super.visitRelationalExpression(ctx)
    }

    val QCParser.ShiftExpressionContext.terminal: Boolean get() = shiftExpression() != null

    override fun visitShiftExpression(ctx: QCParser.ShiftExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.shiftExpression().accept(this)[0]
            val right = ctx.additiveExpression().accept(this)[0]
            // TODO
            return listOf(BinaryExpression.Mul(left as Expression, right as Expression))
        }
        return super.visitShiftExpression(ctx)
    }

    val QCParser.AdditiveExpressionContext.terminal: Boolean get() = additiveExpression() != null

    override fun visitAdditiveExpression(ctx: QCParser.AdditiveExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.additiveExpression().accept(this)[0]
            val right = ctx.multiplicativeExpression().accept(this)[0]
            // TODO
            return listOf(BinaryExpression.Add(left as Expression, right as Expression))
        }
        return super.visitAdditiveExpression(ctx)
    }

    val QCParser.MultiplicativeExpressionContext.terminal: Boolean get() = multiplicativeExpression() != null

    override fun visitMultiplicativeExpression(ctx: QCParser.MultiplicativeExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.multiplicativeExpression().accept(this)[0]
            val right = ctx.castExpression().accept(this)[0]
            // TODO
            return listOf(BinaryExpression.Mul(left as Expression, right as Expression))
        }
        return super.visitMultiplicativeExpression(ctx)
    }

    val QCParser.CastExpressionContext.terminal: Boolean get() = castExpression() != null

    override fun visitCastExpression(ctx: QCParser.CastExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.castExpression().accept(this)[0]
            // TODO
            //            val right = ctx.typeName().accept(this)[0]
            //            return BinaryExpression.Cast(left as Expression, right as Expression)
            return listOf(left)
        }
        return super.visitCastExpression(ctx)
    }

    val QCParser.UnaryExpressionContext.terminal: Boolean get() = unaryExpression() != null

    override fun visitUnaryExpression(ctx: QCParser.UnaryExpressionContext): List<Statement> {
        if (ctx.terminal) {
            val left = ctx.unaryExpression().accept(this)[0]
            // TODO
            return listOf(left)
        }
        val typeName = ctx.typeName()
        if (typeName != null) {
            return listOf(ReferenceExpression("TODO: sizeof(${typeName.getText()})"))
        }
        return super.visitUnaryExpression(ctx)
    }

    override fun visitPostfixPrimary(ctx: QCParser.PostfixPrimaryContext): List<Statement> {
        val left = ctx.primaryExpression().accept(this)[0]
        return listOf(left)
    }

    override fun visitPostfixCall(ctx: QCParser.PostfixCallContext): List<Statement> {
        val left = ctx.postfixExpression().accept(this)[0]

        val right = ctx.argumentExpressionList()
                ?.assignmentExpression()
                ?.flatMap { it.accept(this) }
                ?.filterNotNull()
        return listOf(FunctionCall(left as Expression, right ?: listOf()))
    }

    override fun visitPrimaryExpression(ctx: QCParser.PrimaryExpressionContext): List<Statement> {
        val text = ctx.getText()
        return listOf(when {
            ctx.Identifier() != null -> ReferenceExpression(text)
            ctx.StringLiteral().isNotEmpty() -> {
                val concat = ctx.StringLiteral()
                        .fold("", {(ret: String, string: TerminalNode) ->
                            val s = string.getText()
                            ret + s.substring(1, s.length() - 1)
                        })
                ConstantExpression(concat)
            }
            else -> ConstantExpression(text)
        })
    }
}