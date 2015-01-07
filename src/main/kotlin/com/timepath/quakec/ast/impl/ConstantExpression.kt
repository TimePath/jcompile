package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.ast.Value
import org.antlr.v4.runtime.misc.Utils

import com.timepath.quakec.vm.Instruction

class ConstantExpression(val value: Any?) : Expression() {

    override fun evaluate(): Value = ((value) as Value)

    val instr: Map<Class<*>, Instruction> = mapOf(
            javaClass<String>() to Instruction.STORE_STR,
            javaClass<Float>() to Instruction.STORE_FLOAT
    )

    override fun generate(ctx: GenerationContext): List<IR> {
        return listOf(IR(ret = ctx.registry.register(null, value), dummy = true))
    }

    override val text: String
        get() = Utils.escapeWhitespace(value.toString(), false)
}
