package com.timepath.quakec.compiler.ast

import java.util.Arrays
import com.timepath.quakec.compiler.gen.FunctionIR
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.ReferenceIR
import com.timepath.quakec.vm.Function
import com.timepath.quakec.vm.Instruction

/**
 * Replaced with a number during compilation
 */
class FunctionLiteral(val id: String? = null,
                      val returnType: Type? = null,
                      val argTypes: Array<Type>? = null,
                      c: List<Statement>? = null,
                      val builtin: Int? = null) : Expression() {

    {
        if (c != null) {
            addAll(c)
        }
    }

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to id,
                "type" to returnType,
                "args" to Arrays.toString(argTypes))

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
        val list = (listOf(
                FunctionIR(f))
                + children.flatMap { it.generate(ctx) }
                + IR(instr = Instruction.DONE)
                + ReferenceIR(global.ref))
        ctx.allocator.pop()
        return list
    }
}
