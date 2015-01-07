package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.ast.Type
import java.util.Arrays

/**
 * Replaced with a number during compilation
 */
class FunctionLiteral(val name: String? = null,
                      val returnType: Type? = null,
                      val argTypes: Array<Type>? = null,
                      var block: BlockStatement? = null) : Expression() {

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to name,
                "returnType" to returnType,
                "args" to Arrays.toString(argTypes))

    override fun generate(ctx: GenerationContext): List<IR> {
        if (name!! in ctx.registry) return super.generate(ctx)
        val global = ctx.registry.register(name)
        return (block!!.generate(ctx) + IR(ret = global, dummy = true))
    }

}
