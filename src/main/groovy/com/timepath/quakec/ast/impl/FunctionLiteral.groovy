package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.ast.Type
import groovy.transform.TupleConstructor

/**
 * Replaced with a number during compilation
 */
@TupleConstructor
class FunctionLiteral implements Expression {

    String name
    Type returnType
    Type[] argTypes
    BlockStatement block

    @Override
    String getText() { "${returnType}(${argTypes.join(', ')}) ${block.text}" }

    @Override
    IR[] generate(GenerationContext ctx) {
        if (ctx.registry.has(name)) return
        def global = ctx.registry.put(name, null)
        block.generate(ctx) + new IR(ret: global, dummy: true)
    }
}
