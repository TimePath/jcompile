package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR

open class ReferenceExpression(val id: String) : Expression() {

    override val attributes: Map<String, Any>
        get() = mapOf("id" to id)

    override fun generate(ctx: GenerationContext): List<IR> {
        if (id in ctx.registry)
            return listOf(IR(ret = ctx.registry[id], dummy = true))
        return super.generate(ctx)
    }

    override fun toString(): String = id

}
