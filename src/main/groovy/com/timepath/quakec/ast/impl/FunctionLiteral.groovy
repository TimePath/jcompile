package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.Type
import groovy.transform.TupleConstructor

/**
 * Replaced with a number during compilation
 */
@TupleConstructor
class FunctionLiteral implements Expression {

    Type returnType
    Type[] argTypes
    BlockStatement block

    @Override
    String getText() { "${returnType}(${argTypes.join(', ')}) ${block.text}" }
}
