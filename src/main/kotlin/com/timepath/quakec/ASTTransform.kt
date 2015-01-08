package com.timepath.quakec

import com.timepath.quakec.ast.Statement
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.ast.BlockStatement
import com.timepath.quakec.ast.Expression
import java.util.Stack
import com.timepath.quakec.ast.Type
import com.timepath.quakec.ast.impl.FunctionLiteral
import com.timepath.quakec.ast.impl.DeclarationExpression
import com.timepath.quakec.ast.impl.ReturnStatement
import com.timepath.quakec.ast.impl.ReferenceExpression
import com.timepath.quakec.ast.impl.BinaryExpression
import com.timepath.quakec.ast.impl.ConditionalExpression
import com.timepath.quakec.ast.impl.ConstantExpression
import com.timepath.quakec.ast.impl.FunctionCall

class ASTTransform : QCBaseVisitor<Statement?>() {

    val stack = Stack<Statement>()
    val root = push(BlockStatement())

    fun add(s: Statement) = stack.peek().children.add(s)

    fun push(s: Statement): Statement {
        if (stack.size() > 0)
            add(s)
        return stack.push(s)
    }

    fun pop(): Statement = stack.pop()

    private fun debug(ctx: ParserRuleContext) {
        val token = ctx.start
        val source = token.getTokenSource()

        val line = token.getLine()
        val col = token.getCharPositionInLine()
        val file = source.getSourceName()
        println("I: {$token} $line,$col $file")
    }

    override fun visitCompilationUnit(ctx: QCParser.CompilationUnitContext): Statement {
        super.visitCompilationUnit(ctx)
        return root
    }

    override fun visitCompoundStatement(ctx: QCParser.CompoundStatementContext): Statement? {
        push(BlockStatement())
        super.visitCompoundStatement(ctx)
        return pop()
    }

    override fun visitBlockItemList(ctx: QCParser.BlockItemListContext): Statement? {
        ctx.children?.forEach {
            val statement = it.accept(this)
            if (statement != null && statement !is BlockStatement) {
                add(statement)
            }
        }
        return null
    }

    override fun visitFunctionDefinition(ctx: QCParser.FunctionDefinitionContext): Statement {
        val id = ctx.declarator().getText()
        val functionLiteral = FunctionLiteral(id, Type.Void, array())
        push(functionLiteral)
        super.visitChildren(ctx.compoundStatement())
        return pop()
    }

    override fun visitDeclaration(ctx: QCParser.DeclarationContext): Statement? {
        val declarations = ctx.initDeclaratorList().initDeclarator()
        declarations.forEach {
            val id = it.declarator().getText()
            add(DeclarationExpression(id))
        }
        super.visitDeclaration(ctx)
        return null
    }

    override fun visitJumpStatement(ctx: QCParser.JumpStatementContext): Statement? {
        val expr = ctx.expression()
        if (expr != null) {
            return ReturnStatement(visitExpression(expr) as Expression)
        }
        return ReturnStatement(null) // TODO: break, continue
    }

    override fun visitIterationStatement(ctx: QCParser.IterationStatementContext): Statement? {
        // TODO
        return ConstantExpression("TODO: Iteration")
    }

    override fun visitIfStatement(ctx: QCParser.IfStatementContext): Statement {
        val get = {(i: Int) -> ctx.statement()[i] }
        val test = ctx.expression()
        val yes = get(0)
        val no = when (ctx.statement().size()) {
            1 -> null
            else -> get(1)
        }
        val s = stack.size()
        stack.push(BlockStatement())
        val testExpr = test.accept(this)!!
        val yesExpr = yes.accept(this)!!
        val noExpr = no?.accept(this)
        while (stack.size() > s)
            stack.pop()
        val conditionalExpression = ConditionalExpression(testExpr as Expression, yesExpr, noExpr)
        add(conditionalExpression)
        return conditionalExpression
    }

    override fun visitExpressionStatement(ctx: QCParser.ExpressionStatementContext): Statement? {
        val expr = ctx.expression()
        if (expr != null) {
            return visitExpression(expr)
        }
        // redundant semicolon
        return null
    }

    val QCParser.AssignmentExpressionContext.terminal: Boolean get() = assignmentExpression() != null

    override fun visitAssignmentExpression(ctx: QCParser.AssignmentExpressionContext): Statement? {
        if (ctx.terminal) {
            // TODO
            val left = ReferenceExpression("TODO: lvalue")
            val right = visit(ctx.assignmentExpression())
            return BinaryExpression.Assign(left, right as Expression)
        }
        return super.visitAssignmentExpression(ctx)
    }

    val QCParser.ConditionalExpressionContext.terminal: Boolean get() = expression().isNotEmpty()

    override fun visitConditionalExpression(ctx: QCParser.ConditionalExpressionContext): Statement? {
        if (ctx.terminal) {
            val condition = visit(ctx.logicalOrExpression())
            val yes = visit(ctx.expression(0))
            val no = visit(ctx.expression(1))
            return ConditionalExpression(condition as Expression, yes!!, no)
        }
        return super.visitConditionalExpression(ctx)
    }

    val QCParser.LogicalOrExpressionContext.terminal: Boolean get() = logicalOrExpression() != null

    override fun visitLogicalOrExpression(ctx: QCParser.LogicalOrExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.logicalOrExpression())
            val right = visit(ctx.logicalAndExpression())
            return BinaryExpression.Or(left as Expression, right as Expression)
        }
        return super.visitLogicalOrExpression(ctx)
    }

    val QCParser.LogicalAndExpressionContext.terminal: Boolean get() = logicalAndExpression() != null

    override fun visitLogicalAndExpression(ctx: QCParser.LogicalAndExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.logicalAndExpression())
            val right = visit(ctx.inclusiveOrExpression())
            return BinaryExpression.And(left as Expression, right as Expression)
        }
        return super.visitLogicalAndExpression(ctx)
    }

    val QCParser.InclusiveOrExpressionContext.terminal: Boolean get() = inclusiveOrExpression() != null

    override fun visitInclusiveOrExpression(ctx: QCParser.InclusiveOrExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.inclusiveOrExpression())
            val right = visit(ctx.exclusiveOrExpression())
            return BinaryExpression.BitOr(left as Expression, right as Expression)
        }
        return super.visitInclusiveOrExpression(ctx)
    }

    val QCParser.ExclusiveOrExpressionContext.terminal: Boolean get() = exclusiveOrExpression() != null

    override fun visitExclusiveOrExpression(ctx: QCParser.ExclusiveOrExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.exclusiveOrExpression())
            val right = visit(ctx.andExpression())
            return BinaryExpression.BitXor(left as Expression, right as Expression)
        }
        return super.visitExclusiveOrExpression(ctx)
    }

    val QCParser.AndExpressionContext.terminal: Boolean get() = andExpression() != null

    override fun visitAndExpression(ctx: QCParser.AndExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.andExpression())
            val right = visit(ctx.equalityExpression())
            return BinaryExpression.BitAnd(left as Expression, right as Expression)
        }
        return super.visitAndExpression(ctx)
    }

    val QCParser.EqualityExpressionContext.terminal: Boolean get() = equalityExpression() != null

    override fun visitEqualityExpression(ctx: QCParser.EqualityExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.equalityExpression())
            val right = visit(ctx.relationalExpression())
            // TODO
            return BinaryExpression.Eq(left as Expression, right as Expression)
        }
        return super.visitEqualityExpression(ctx)
    }

    val QCParser.RelationalExpressionContext.terminal: Boolean get() = relationalExpression() != null

    override fun visitRelationalExpression(ctx: QCParser.RelationalExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.relationalExpression())
            val right = visit(ctx.shiftExpression())
            // TODO
            return BinaryExpression.Le(left as Expression, right as Expression)
        }
        return super.visitRelationalExpression(ctx)
    }

    val QCParser.ShiftExpressionContext.terminal: Boolean get() = shiftExpression() != null

    override fun visitShiftExpression(ctx: QCParser.ShiftExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.shiftExpression())
            val right = visit(ctx.additiveExpression())
            // TODO
            return BinaryExpression.Mul(left as Expression, right as Expression)
        }
        return super.visitShiftExpression(ctx)
    }

    val QCParser.AdditiveExpressionContext.terminal: Boolean get() = additiveExpression() != null

    override fun visitAdditiveExpression(ctx: QCParser.AdditiveExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.additiveExpression())
            val right = visit(ctx.multiplicativeExpression())
            // TODO
            return BinaryExpression.Add(left as Expression, right as Expression)
        }
        return super.visitAdditiveExpression(ctx)
    }

    val QCParser.MultiplicativeExpressionContext.terminal: Boolean get() = multiplicativeExpression() != null

    override fun visitMultiplicativeExpression(ctx: QCParser.MultiplicativeExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.multiplicativeExpression())
            val right = visit(ctx.castExpression())
            // TODO
            return BinaryExpression.Mul(left as Expression, right as Expression)
        }
        return super.visitMultiplicativeExpression(ctx)
    }

    val QCParser.CastExpressionContext.terminal: Boolean get() = castExpression() != null

    override fun visitCastExpression(ctx: QCParser.CastExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.castExpression())
            val right = visit(ctx.typeName())
            // TODO
            //            return BinaryExpression.Cast(left as Expression, right as Expression)
            return left
        }
        return super.visitCastExpression(ctx)
    }

    val QCParser.UnaryExpressionContext.terminal: Boolean get() = unaryExpression() != null

    override fun visitUnaryExpression(ctx: QCParser.UnaryExpressionContext): Statement? {
        if (ctx.terminal) {
            val left = visit(ctx.unaryExpression())
            // TODO
            return left
        }
        val typeName = ctx.typeName()
        if (typeName != null) {
            return ReferenceExpression("TODO: sizeof(${typeName.getText()})")
        }
        return super.visitUnaryExpression(ctx)
    }

    override fun visitPostfixPrimary(ctx: QCParser.PostfixPrimaryContext): Statement? {
        val left = visit(ctx.primaryExpression())
        return left
    }

    override fun visitPostfixCall(ctx: QCParser.PostfixCallContext): Statement? {
        val left = visit(ctx.postfixExpression())
        val functionCall = FunctionCall(left as Expression)
        val right = ctx.argumentExpressionList()
                ?.assignmentExpression()
                ?.map { visit(it) }
                ?.filterNotNull()
        if (right != null) {
            functionCall.children.addAll(right)
        }
        return functionCall
    }

    override fun visitPrimaryExpression(ctx: QCParser.PrimaryExpressionContext): Statement {
        return ConstantExpression(ctx.getText())
    }
}