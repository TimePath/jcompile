package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import groovy.transform.TupleConstructor

@TupleConstructor
class ReferenceExpression implements Expression {

    String id

    @Override
    String getText() { id }

    @Override
    def generate(GenerationContext ctx) {
        [ctx.allocate(text)]
    }
}
