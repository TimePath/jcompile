package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.Value
import groovy.transform.TupleConstructor

@TupleConstructor
class FunctionCall implements Expression {

    Expression function
    Expression[] args

    @Override
    Value evaluate() { null }

    @Override
    boolean hasSideEffects() { false }

    @Override
    String getText() { "${function.text}(${args*.text.join(', ')})" }
}
