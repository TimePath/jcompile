package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

public class SwitchExpression(val test: Expression, add: List<Expression>, override val ctx: PRC? = null) : Expression() {

    init {
        addAll(add)
    }

    override val simpleName = "SwitchExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    class Case(
            /**
             * Case expression, null = default
             */
            val expr: Expression?,
            override val ctx: PRC? = null) : Expression() {
        override val simpleName = "Case"
        override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    }

}
