package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.ast.Value
import com.timepath.quakec.vm.Instruction
import groovy.transform.TupleConstructor

@TupleConstructor
class FunctionCall implements Expression {

    Expression function
    Expression[] args = []

    @Override
    Value evaluate() { null }

    @Override
    boolean hasSideEffects() { false }

    @Override
    String getText() { "#${function.text}(${args*.text.join(', ')})" }

    @Override
    IR[] generate(GenerationContext ctx) {
        def args = args*.generate(ctx)
        def instr = { int i ->
            Instruction.from((Instruction.CALL0.ordinal() + i))
        }
        int i = 0
        def prepare = args.collect {
            def param = Instruction.OFS_PARM0 + (3 * i++)
            new IR(Instruction.STORE_FLOAT, (int[]) [it[-1].ret, param], param)
        }
        args.flatten() + prepare + new IR(instr(i), (int[]) [function.generate(ctx)[-1].ret], Instruction.OFS_RETURN)
    }
}
