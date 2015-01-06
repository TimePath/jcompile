package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR

class DeclarationExpression(id: String) : ReferenceExpression(id) {

    override val text: String
        get() = id

    override fun generate(ctx: GenerationContext): List<IR> {
        if (super.generate(ctx).isNotEmpty()) return super.generate(ctx)
        val global = ctx.registry.register(this.id)
        return listOf(IR(ret = global, dummy = true))
    }

}
