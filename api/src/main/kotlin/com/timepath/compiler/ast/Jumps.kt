package com.timepath.compiler.ast

import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.compiler.Type
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

// TODO: conditional goto
class GotoExpression(val id: String, ctx: ParserRuleContext? = null) : Expression(ctx) {
    override fun type(gen: Generator) = throw UnsupportedOperationException()

    override val attributes: Map<String, Any?>
        get() = mapOf("label" to id)

    override fun toString(): String = "goto $id"

    override fun generate(gen: Generator): List<IR> {
        // filled in by new labels
        val instr = IR(Instruction.GOTO, array(0, 0, 0))
        gen.gotoLabels[instr] = id
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

    override fun type(gen: Generator) = returnValue?.type(gen) ?: Type.Void

    override fun generate(gen: Generator): List<IR> {
        val genRet = returnValue?.doGenerate(gen)
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
    override fun type(gen: Generator) = throw UnsupportedOperationException()

    override fun toString(): String = "continue"

    override fun generate(gen: Generator): List<IR> {
        // filled in by Loop.doGenerate()
        return listOf(IR(Instruction.GOTO, array(0, 0, 0)))
    }
}

// TODO: on labels
class BreakStatement(ctx: ParserRuleContext? = null) : Expression(ctx) {
    override fun type(gen: Generator) = throw UnsupportedOperationException()

    override fun toString(): String = "break"

    override fun generate(gen: Generator): List<IR> {
        // filled in by Loop.doGenerate()
        return listOf(IR(Instruction.GOTO, array(0, 1, 0)))
    }
}
