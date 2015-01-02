package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.Value
import com.timepath.quakec.vm.Instruction
import groovy.transform.TupleConstructor
import org.antlr.v4.runtime.misc.Utils

@TupleConstructor
class ConstantExpression implements Expression {

    def value

    @Override
    Value evaluate() { value }

    @Override
    boolean hasSideEffects() { false }

    private def instr = [
            (String) : Instruction.STORE_STR,
            (Integer): Instruction.STORE_FLOAT,
            (Float)  : Instruction.STORE_FLOAT
    ]

    @Override
    def generate(GenerationContext ctx) {
        def allocate = ctx.allocate(text)
        [instr[value.class], value, allocate, allocate]
    }

    @Override
    String getText() { Utils.escapeWhitespace(value as String, false) }
}
