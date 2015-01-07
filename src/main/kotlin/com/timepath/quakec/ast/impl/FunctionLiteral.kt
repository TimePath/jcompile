package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.ast.Type
import java.util.Arrays
import com.timepath.quakec.ast.Statement

/**
 * Replaced with a number during compilation
 */
class FunctionLiteral(val id: String? = null,
                      val returnType: Type? = null,
                      val argTypes: Array<Type>? = null) : Expression() {

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to id,
                "returnType" to returnType,
                "args" to Arrays.toString(argTypes))

}
