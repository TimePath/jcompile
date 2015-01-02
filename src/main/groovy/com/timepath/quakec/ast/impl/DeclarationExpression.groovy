package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR

class DeclarationExpression extends ReferenceExpression {

    DeclarationExpression(String id) {
        this.id = id
    }

    @Override
    String getText() { "$id" }

    @Override
    IR[] generate(GenerationContext ctx) {
        if (super.generate(ctx)) return
        def global = ctx.registry.put(this.id, null)
        new IR(ret: global, dummy: true)
    }
}
