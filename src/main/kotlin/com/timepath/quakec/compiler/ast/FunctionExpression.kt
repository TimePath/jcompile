package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.Type
import com.timepath.quakec.compiler.gen.FunctionIR
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.LabelIR
import com.timepath.quakec.compiler.gen.ReferenceIR
import com.timepath.quakec.vm.Function
import com.timepath.quakec.vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Replaced with a number during compilation
 */
class FunctionExpression(val id: String? = null,
                         val signature: Type.Function,
                         val params: List<Expression>? = null,
                         val vararg: Expression? = null,
                         add: List<Expression>? = null,
                         val builtin: Int? = null,
                         ctx: ParserRuleContext? = null) : Expression(ctx) {

    {
        if (add != null) {
            addAll(add)
        }
    }

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to id,
                "type" to signature)

    override fun generate(ctx: Generator): List<IR> {
        if (id != null && id in ctx.allocator) {
            Generator.logger.warning("redefining $id")
        }

        val global = ctx.allocator.allocateFunction(id)
        val f = Function(
                firstStatement = if (builtin == null)
                    0 // to be filled in later
                else
                    -builtin,
                firstLocal = 0,
                numLocals = 0,
                profiling = 0,
                nameOffset = ctx.allocator.allocateString(id!!).ref,
                fileNameOffset = 0,
                numParams = 0,
                sizeof = byteArray(0, 0, 0, 0, 0, 0, 0, 0)
        )
        ctx.allocator.push(id)
        val params = with(linkedListOf<Expression>()) {
            params?.let { addAll(it) }
            vararg?.let { add(it) }
            this
        }
        val genParams = params.flatMap { it.doGenerate(ctx) }
        val children = children.flatMap { it.doGenerate(ctx) }
        run {
            // Calculate label jumps
            val labelIndices = linkedMapOf<String, Int>()
            val jumpIndices = linkedMapOf<String, Int>()
            children.fold(0, { i, it ->
                when {
                    it is LabelIR -> {
                        labelIndices[it.id] = i
                    }
                    it.instr == Instruction.GOTO && it.args[0] == 0 -> {
                        jumpIndices[ctx.gotoLabels[it]] = i
                    }
                }
                if (it.real) i + 1 else i
            })
            val real = children.filter { it.real }
            for ((s, i) in jumpIndices) {
                real[i].args[0] = labelIndices[s] - i
            }
        }
        val list = (listOf(
                FunctionIR(f))
                + genParams
                + children
                + IR(instr = Instruction.DONE)
                + ReferenceIR(global.ref))
        ctx.allocator.pop()
        return list
    }
}
