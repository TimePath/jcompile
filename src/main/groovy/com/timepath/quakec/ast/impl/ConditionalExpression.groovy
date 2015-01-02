package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.Value
import groovy.transform.TupleConstructor

@TupleConstructor
class ConditionalExpression implements Expression {

    Expression test, yes, no

    @Override
    Value evaluate() {
        def result = test.evaluate()
        if (result == null) return null
        result ? yes.evaluate() : no.evaluate()
    }

    @Override
    boolean hasSideEffects() { false }

    @Override
    def generate() { null }

    @Override
    String getText() { "(${test.text} ? ${yes.text} : ${no.text})" }
}
