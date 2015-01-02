package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression as rvalue
import com.timepath.quakec.ast.impl.ReferenceExpression as lvalue
import groovy.transform.InheritConstructors
import groovy.transform.TupleConstructor

@TupleConstructor
abstract class BinaryExpression<L extends rvalue, R extends rvalue> implements rvalue {

    L left
    R right

    String getText(String op) { "(${left.text} $op ${right.text})" }

    @InheritConstructors
    static class Assign extends BinaryExpression<lvalue, rvalue> {

        @Override
        String getText() { getText '=' }
    }

    @InheritConstructors
    static class Add extends BinaryExpression<rvalue, rvalue> {
        @Override
        String getText() { getText '+' }
    }

    @InheritConstructors
    static class Sub extends BinaryExpression<rvalue, rvalue> {
        @Override
        String getText() { getText '-' }
    }
}


