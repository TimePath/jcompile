package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

public class SwitchExpression(val test: Expression, add: List<Expression>, override val ctx: PRC? = null) : Expression() {
    override fun withChildren(children: List<Expression>) = copy(children = children)

    fun copy(
            test: Expression = this.test,
            children: List<Expression> = this.children,
            ctx: PRC? = this.ctx
    ) = SwitchExpression(test, children, ctx)

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
            override val ctx: PRC? = null) : Expression() {
        override val simpleName = "Case"
        override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    }

}
