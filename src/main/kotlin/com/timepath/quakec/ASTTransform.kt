package com.timepath.quakec

import java.util.Stack
import com.timepath.quakec.ast.BlockStatement
import com.timepath.quakec.ast.Statement
import com.timepath.quakec.ast.impl.FunctionLiteral
import com.timepath.quakec.ast.Type
import com.timepath.quakec.ast.impl.DeclarationExpression
import com.timepath.quakec.ast.impl.ConditionalExpression
import com.timepath.quakec.ast.impl.ConstantExpression
import com.timepath.quakec.ast.Expression
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.ast.impl.ReturnStatement

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

    override fun visitCompoundStatement(ctx: QCParser.CompoundStatementContext): Statement {
        push(BlockStatement())
        super.visitCompoundStatement(ctx)
        return pop()
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

    override fun visitExpressionStatement(ctx: QCParser.ExpressionStatementContext): Statement? {
        val expr = ctx.expression()
        if (expr != null) {
            return visitExpression(expr)
        }
        // redundant semicolon
        return null
    }

    override fun visitExpression(ctx: QCParser.ExpressionContext): Statement {
        // TODO
        return ConstantExpression(ctx)
    }

    override fun visitIterationStatement(ctx: QCParser.IterationStatementContext): Statement? {
        // TODO
        return ConstantExpression(ctx)
    }

    override fun visitIfStatement(ctx: QCParser.IfStatementContext): Statement {
        val get = {(i: Int) -> ctx.statement()[i] }
        val test = ctx.expression()
        val yes = get(0)
        val no = when (ctx.statement().size()) {
            1 -> null
            else -> get(1)
        }
        stack.push(BlockStatement())
        val testExpr = test.accept(this)!!
        val yesExpr = yes.accept(this)!!
        val noExpr = no?.accept(this)
        stack.pop()
        val conditionalExpression = ConditionalExpression(testExpr as Expression, yesExpr, noExpr)
        add(conditionalExpression)
        return conditionalExpression
    }
}
