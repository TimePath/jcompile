package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.Value
import groovy.transform.TupleConstructor
import org.antlr.v4.runtime.misc.Utils

@TupleConstructor
class ConstantExpression implements Expression {

    def value

    @Override
    Value evaluate() { value }

    @Override
    boolean hasSideEffects() { false }

    @Override
    def generate() { null }

    @Override
    String getText() {
        value instanceof String ?
                '"' + Utils.escapeWhitespace(value, false) + '"'
                : value
    }
}
