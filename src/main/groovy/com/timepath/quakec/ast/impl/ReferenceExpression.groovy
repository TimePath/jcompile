package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import groovy.transform.TupleConstructor
import org.jetbrains.annotations.NotNull

@TupleConstructor
class ReferenceExpression implements Expression {

    @NotNull
    String id

    @Override
    String getText() { id }

    @Override
    IR[] generate(GenerationContext ctx) {
        ctx.registry.has(id) ?
                new IR(ret: ctx.registry.get(id), dummy: true)
                : null
    }
}
