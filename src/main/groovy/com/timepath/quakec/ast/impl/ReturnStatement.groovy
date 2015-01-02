package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.ast.Statement
import com.timepath.quakec.vm.Instruction
import groovy.transform.TupleConstructor

/**
 * Return can be assigned to, and has a constant address
 */
@TupleConstructor
class ReturnStatement implements Statement {

    @Override
    String getText() { "return '\$1 \$2 \$3'" }

    @Override
    IR[] generate(GenerationContext ctx) {
        new IR(Instruction.RETURN, (int[]) [0, 0, 0], 0)
    }
}
