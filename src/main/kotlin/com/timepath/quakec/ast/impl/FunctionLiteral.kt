package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.ast.Type

/**
 * Replaced with a number during compilation
 */
class FunctionLiteral(val name: String? = null,
                      val returnType: Type? = null,
                      val argTypes: Array<Type>? = null,
                      val block: BlockStatement? = null) : Expression {

    override val text: String
        get() = "${returnType}(${argTypes!!.map { it.toString() }.join(", ")}) ${block!!.text}"

    override fun generate(ctx: GenerationContext): List<IR> {
        if (name!! in ctx.registry) return super.generate(ctx)
        val global = ctx.registry.register(name)
        return (block!!.generate(ctx) + IR(ret = global, dummy = true))
    }

}
