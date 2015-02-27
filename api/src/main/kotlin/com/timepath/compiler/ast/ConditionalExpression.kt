package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext

class ConditionalExpression(val test: Expression,
                            val expression: Boolean,
                            val pass: Expression,
                            val fail: Expression? = null,
                            override val ctx: ParserRuleContext? = null) : Expression() {

    {
        add(test)
        add(pass)
        if (fail != null) {
            add(fail)
        }
    }
    override val simpleName = "ConditionalExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = "($test ? $pass : $fail)"

}
