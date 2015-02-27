package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext

class BlockExpression(add: List<Expression>? = null, override val ctx: ParserRuleContext? = null) : Expression() {
    {
        if (add != null) {
            addAll(add)
        }
    }
    override val simpleName = "BlockExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
}
