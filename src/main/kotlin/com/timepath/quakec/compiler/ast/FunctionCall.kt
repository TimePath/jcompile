package com.timepath.quakec.compiler.ast

import kotlin.properties.Delegates

class FunctionCall(val function: Expression,
                   c: List<Statement>? = null) : Expression() {

    {
        if (c != null) {
            addAll(c)
        }
    }

    val args: List<Expression> by Delegates.lazy {
        children.filterIsInstance<Expression>()
    }

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to function)

    override fun toString(): String = "$function(${args.joinToString(", ")})"

}
