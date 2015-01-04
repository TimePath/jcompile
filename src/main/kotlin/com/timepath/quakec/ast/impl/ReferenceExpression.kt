package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import java.util.LinkedList

open class ReferenceExpression(val id: String) : Expression {

    override val text: String = id

    override fun generate(ctx: GenerationContext): List<IR> {
        if (id in ctx.registry)
            return listOf(IR(ret = ctx.registry[id], dummy = true))
        return super.generate(ctx)
    }

}
