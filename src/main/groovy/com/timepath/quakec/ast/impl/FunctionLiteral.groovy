package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Type
import groovy.transform.TupleConstructor

/**
 * Replaced with a number during compilation
 */
@TupleConstructor
class FunctionLiteral extends ReferenceExpression {

    Type returnType
    Type[] argTypes
    BlockStatement block

    @Override
    String getText() { "${returnType}(${argTypes.join(', ')}) ${block.text}" }
}
