package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.ast.Statement
import groovy.transform.TupleConstructor

@TupleConstructor
class BlockStatement implements Statement {

    Statement[] children = []

    @Override
    String getText() { "{\n${children*.text.join('\n')}\n}" }

    @Override
    IR[] generate(GenerationContext ctx) {
        children.collect { it.generate(ctx) }.flatten()
    }
}
