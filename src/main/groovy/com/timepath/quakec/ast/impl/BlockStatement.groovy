package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.Statement
import groovy.transform.TupleConstructor

@TupleConstructor
class BlockStatement implements Statement {

    Statement[] children

    @Override
    String getText() { "{\n${children*.text.join('\n')}\n}" }

    @Override
    def generate(GenerationContext ctx) {
        children*.generate(ctx)
    }
}
