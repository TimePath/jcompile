package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import groovy.transform.TupleConstructor

@TupleConstructor
class ReferenceExpression implements Expression {

    String id

    @Override
    String getText() { id }
}
