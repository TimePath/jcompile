package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.vm.Instruction

class FunctionCall(val function: Expression? = null,
                   vararg val args: Expression) : Expression {

    override val text: String
        get() = "#${function!!.text}(${args.map { text }.join(", ")})"

    override fun generate(ctx: GenerationContext): List<IR> {
        val args = args.map { it.generate(ctx) }
        val instr = {(i: Int) ->
            Instruction.from(Instruction.CALL0.ordinal() + i)
        }
        var i = 0
        val prepare: List<IR> = args.map {
            val param = Instruction.OFS_PARM0 + (3 * i++)
            IR(Instruction.STORE_FLOAT, array(it[-1].ret, param), param)
        }
        return (args.flatMap { it }
                + prepare
                + listOf(IR(instr(i), array(function!!.generate(ctx)[-1].ret), Instruction.OFS_RETURN))
                )
    }

}
