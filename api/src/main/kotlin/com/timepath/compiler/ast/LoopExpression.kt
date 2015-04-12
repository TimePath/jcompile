package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext

class LoopExpression(val predicate: Expression,
                     body: Expression,
                     val checkBefore: Boolean = true,
                     val initializer: List<Expression>? = null,
                     val update: List<Expression>? = null,
                     override val ctx: ParserRuleContext? = null) : Expression() {
    init {
        add(body)
    }
    override val simpleName = "LoopExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

}
