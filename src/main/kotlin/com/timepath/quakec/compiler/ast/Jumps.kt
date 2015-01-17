package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.vm.Instruction

/**
 * Return can be assigned to, and has a constant address
 */
class ReturnStatement(val returnValue: Expression?) : Statement() {
    override fun generate(ctx: Generator): List<IR> {
        val genRet = returnValue?.generate(ctx)
        val ret = linkedListOf<IR>()
        val args = array(0, 0, 0)
        if (genRet != null) {
            ret.addAll(genRet)
            args[0] = genRet.last().ret
        }
        ret.add(IR(Instruction.RETURN, args, 0))
        return ret
    }
}

// TODO: labels
class ContinueStatement() : Statement() {
    override fun generate(ctx: Generator): List<IR> {
        // filled in by Loop.generate()
        return listOf(IR(Instruction.GOTO, array(0, 0, 0)))
    }
}
class BreakStatement() : Statement() {
    override fun generate(ctx: Generator): List<IR> {
        // filled in by Loop.generate()
        return listOf(IR(Instruction.GOTO, array(0, 1, 0)))
    }
}
class GotoStatement(val id: String) : Statement() {
    override val attributes: Map<String, Any?>
        get() = mapOf("label" to id)

    override fun generate(ctx: Generator): List<IR> {
        // filled in by new labels
        val instr = IR(Instruction.GOTO, array(0, 0, 0))
        ctx.gotoLabels[instr] = id
        return listOf(instr)
    }
}