package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Statement
import groovy.transform.TupleConstructor

/**
 * Return can be assigned to, and has a constant address
 */
@TupleConstructor
class ReturnStatement implements Statement {

    @Override
    String getText() { "return '\$1 \$2 \$3'" }
}
