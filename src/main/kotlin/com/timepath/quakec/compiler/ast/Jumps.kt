package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

class GotoExpression(val id: String, ctx: ParserRuleContext? = null) : Expression(ctx) {
    override val attributes: Map<String, Any?>
        get() = mapOf("label" to id)

    override fun toString(): String = "goto $id"

    override fun generate(ctx: Generator): List<IR> {
        // filled in by new labels
        val instr = IR(Instruction.GOTO, array(0, 0, 0))
        ctx.gotoLabels[instr] = id
        return listOf(instr)
    }
}

/**
 * Return can be assigned to, and has a constant address
 */
class ReturnStatement(val returnValue: Expression?, ctx: ParserRuleContext? = null) : Expression(ctx) {
    {
        if (returnValue != null) {
            add(returnValue)
        }
    }
    override fun generate(ctx: Generator): List<IR> {
        val genRet = returnValue?.doGenerate(ctx)
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
// TODO: on labels
class ContinueStatement(ctx: ParserRuleContext? = null) : Expression(ctx) {

    override fun toString(): String = "continue"

    override fun generate(ctx: Generator): List<IR> {
        // filled in by Loop.doGenerate()
        return listOf(IR(Instruction.GOTO, array(0, 0, 0)))
    }
}
class BreakStatement(ctx: ParserRuleContext? = null) : Expression(ctx) {

    override fun toString(): String = "break"

    override fun generate(ctx: Generator): List<IR> {
        // filled in by Loop.doGenerate()
        return listOf(IR(Instruction.GOTO, array(0, 1, 0)))
    }
}
