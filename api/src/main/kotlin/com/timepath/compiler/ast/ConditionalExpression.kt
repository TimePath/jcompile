package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

public class ConditionalExpression(val test: Expression,
                                   val expression: Boolean,
                                   val pass: Expression,
                                   val fail: Expression? = null,
                                   override val ctx: PRC? = null) : Expression() {

    override fun withChildren(children: List<Expression>) = require(children!!.size() in 2..3) let {
        val (test, pass) = children
        copy(test = test, pass = pass, fail = if (children.size() == 3) children[2] else null)
    }

    fun copy(test: Expression = this.test,
             expression: Boolean = this.expression,
             pass: Expression = this.pass,
             fail: Expression? = this.fail,
             ctx: PRC? = this.ctx
    ) = ConditionalExpression(test, expression, pass, fail, ctx)

    init {
        add(test)
        add(pass)
        fail?.let { add(it) }
    }

    override val simpleName = "ConditionalExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = "($test ? $pass : $fail)"

}
