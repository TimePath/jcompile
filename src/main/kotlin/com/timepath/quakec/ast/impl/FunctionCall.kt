package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.vm.Instruction
import kotlin.properties.Delegates

class FunctionCall(val function: Expression? = null) : Expression() {

    val args: List<Expression> by Delegates.lazy {
        children.filterIsInstance<Expression>()
    }

    override val text: String
        get() = "#${function!!.text}(${args.map { it.text }.join(", ")})"

    override fun generate(ctx: GenerationContext): List<IR> {
        val args = args.map { it.generate(ctx) }
        val instr = {(i: Int) ->
            Instruction.from(Instruction.CALL0.ordinal() + i)
        }
        var i = 0
        val prepare: List<IR> = args.map {
            val param = Instruction.OFS_PARAM(i)
            IR(Instruction.STORE_FLOAT, array(it.last().ret, param), param)
        }
        return (args.flatMap { it }
                + prepare
                + listOf(IR(instr(i), array(function!!.generate(ctx).last().ret), Instruction.OFS_PARAM(-1)))
                )
    }

}
