package com.timepath.quakec.compiler.ast

import kotlin.properties.Delegates
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.vm.Instruction

class FunctionCall(val function: Expression,
                   c: List<Statement>? = null) : Expression() {

    {
        if (c != null) {
            addAll(c)
        }
    }

    val args: List<Expression> by Delegates.lazy {
        children.filterIsInstance<Expression>()
    }

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to function)

    override fun toString(): String = "$function(${args.joinToString(", ")})"

    override fun generate(ctx: Generator): List<IR> {
        // TODO: increase this
        if (args.size() > 8) {
            Generator.logger.warning("${function} takes ${args.size()} parameters")
        }
        val args = args.take(8).map { it.generate(ctx) }
        val instr = {(i: Int) ->
            Instruction.from(Instruction.CALL0.ordinal() + i)
        }
        var i = 0
        val prepare: List<IR> = args.map {
            val param = Instruction.OFS_PARAM(i++)
            IR(Instruction.STORE_FLOAT, array(it.last().ret, param), param, "Prepare param $i")
        }
        val genF = function.generate(ctx)
        val funcId = genF.last().ret
        val global = ctx.allocator.allocateReference()
        val ret = linkedListOf<IR>()
        ret.addAll(args.flatMap { it })
        ret.addAll(prepare)
        ret.add(IR(instr(i), array(funcId), Instruction.OFS_PARAM(-1)))
        ret.add(IR(Instruction.STORE_FLOAT, array(Instruction.OFS_PARAM(-1), global.ref), global.ref, "Save response"))
        return ret
    }
}
