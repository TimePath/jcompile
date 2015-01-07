package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR

class DeclarationExpression(id: String) : ReferenceExpression(id) {

    override val attributes: Map<String, Any>
        get() = mapOf("id" to id)

    override fun generate(ctx: GenerationContext): List<IR> {
        if (super.generate(ctx).isNotEmpty()) return super.generate(ctx)
        val global = ctx.registry.register(this.id)
        return listOf(IR(ret = global, dummy = true))
    }

}
