package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Expression
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.ast.Value
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR

class ConstantExpression(any: Any) : Expression() {

    val instr: Map<Class<*>, Instruction> = mapOf(
            javaClass<String>() to Instruction.STORE_STR,
            javaClass<Float>() to Instruction.STORE_FLOAT
    )

    val value = Value(any)

    override val attributes: Map<String, Any?>
        get() = mapOf("value" to value)

    override fun evaluate(): Value = value

    override fun generate(ctx: GenerationContext): List<IR> {
        return listOf(IR(ret = ctx.registry.register(null, value), dummy = true))
    }

    override fun toString(): String = value.toString()
}

open class ReferenceExpression(val id: String) : Expression() {

    override val attributes: Map<String, Any>
        get() = mapOf("id" to id)

    override fun generate(ctx: GenerationContext): List<IR> {
        if (id in ctx.registry)
            return listOf(IR(ret = ctx.registry[id], dummy = true))
        return super.generate(ctx)
    }

    override fun toString(): String = id

}