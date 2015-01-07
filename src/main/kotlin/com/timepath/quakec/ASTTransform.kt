package com.timepath.quakec

import java.util.Stack
import com.timepath.quakec.ast.BlockStatement
import com.timepath.quakec.ast.Statement
import com.timepath.quakec.ast.impl.FunctionLiteral
import com.timepath.quakec.ast.Type
import com.timepath.quakec.ast.impl.DeclarationExpression
import com.timepath.quakec.ast.impl.ConditionalExpression
import com.timepath.quakec.ast.impl.ConstantExpression

class ASTTransform : QCBaseVisitor<Unit>() {

    val stack = Stack<Statement>()
    val root = push(BlockStatement())

    fun add(s: Statement) {
        stack.peek().children.add(s)
    }

    fun push(s: Statement): Statement {
        if (stack.size() > 0)
            add(s)
        stack.push(s)
        return s
    }

    fun pop() {
        stack.pop()
    }

    override fun toString(): String {
        return root.toStringRecursive()
    }

    override fun visitCompoundStatement(ctx: QCParser.CompoundStatementContext) {
        push(BlockStatement())
        super.visitCompoundStatement(ctx)
        pop()
    }

    override fun visitFunctionDefinition(ctx: QCParser.FunctionDefinitionContext) {
        val id = ctx.declarator().getText()
        val functionLiteral = FunctionLiteral(id, Type.Void, array())
        push(functionLiteral)
        super.visitChildren(ctx.compoundStatement())
        pop()
    }

    override fun visitDeclaration(ctx: QCParser.DeclarationContext) {
        val declarations = ctx.initDeclaratorList().initDeclarator()
        declarations.forEach {
            val id = it.declarator().getText()
            add(DeclarationExpression(id))
        }
        super.visitDeclaration(ctx)
    }

    override fun visitIfStatement(ctx: QCParser.IfStatementContext) {
        val get = {(i: Int) -> ctx.statement()[i] }
        val test = ctx.expression()
        val yes = get(0)
        val no = when (ctx.statement().size()) {
            1 -> null
            else -> get(1)
        }
        // TODO: return Statement instead of Unit
        val yesExpr = ConstantExpression(yes)
        val noExpr = if (no != null) ConstantExpression(no) else null
        val conditionalExpression = ConditionalExpression(ConstantExpression(test), yesExpr, noExpr)
        push(conditionalExpression)
        push(yesExpr)
        yes.accept(this)
        pop()
        if (noExpr != null) {
            push(noExpr)
            no?.accept(this)
            pop()
        }
        pop()
    }
}
