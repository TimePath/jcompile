package com.timepath.quakec.compiler.ast

import java.util.Arrays

/**
 * Replaced with a number during compilation
 */
class FunctionLiteral(val id: String? = null,
                      val returnType: Type? = null,
                      val argTypes: Array<Type>? = null,
                      c: List<Statement>? = null) : Expression() {

    {
        if (c != null) {
            addAll(c)
        }
    }

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to id,
                "type" to returnType,
                "args" to Arrays.toString(argTypes))

}
