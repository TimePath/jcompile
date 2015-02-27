package com.timepath.compiler.ast

import kotlin.properties.Delegates
import org.antlr.v4.runtime.ParserRuleContext

class MethodCallExpression(val function: Expression,
                           add: List<Expression>? = null,
                           override val ctx: ParserRuleContext? = null) : Expression() {

    {
        if (add != null) {
            addAll(add)
        }
    }
    override val simpleName = "MethodCallExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    val args: List<Expression> by Delegates.lazy {
        children.filterIsInstance<Expression>()
    }

    override fun toString(): String = "$function(${args.joinToString(", ")})"

}
