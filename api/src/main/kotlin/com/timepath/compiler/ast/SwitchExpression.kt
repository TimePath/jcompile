package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext

class SwitchExpression(val test: Expression, add: List<Expression>, override val ctx: ParserRuleContext? = null) : Expression() {

    init {
        addAll(add)
    }

    override val simpleName = "SwitchExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    class Case(
            /**
             * Case expression, null = default
             */
            val expr: Expression?,
            override val ctx: ParserRuleContext? = null) : Expression() {
        override val simpleName = "Case"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    }

}
