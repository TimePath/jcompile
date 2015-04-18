package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

public class ConditionalExpression(val test: Expression,
                                   val expression: Boolean,
                                   val pass: Expression,
                                   val fail: Expression? = null,
                                   override val ctx: PRC? = null) : Expression() {

    init {
        add(test)
        add(pass)
        fail?.let { add(it) }
    }

    override val simpleName = "ConditionalExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = "($test ? $pass : $fail)"

}
