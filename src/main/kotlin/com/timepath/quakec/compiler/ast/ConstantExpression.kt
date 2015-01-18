package com.timepath.quakec.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.ReferenceIR
import com.timepath.quakec.compiler.Value

class ConstantExpression(any: Any, ctx: ParserRuleContext? = null) : Expression(ctx) {

    val value = Value(any)

    override val attributes: Map<String, Any?>
        get() = mapOf("value" to value)

    override fun evaluate(): Value = value

    override fun toString(): String = value.toString()

    override fun generate(ctx: Generator): List<IR> {
        val constant = ctx.allocator.allocateConstant(value)
        return listOf(ReferenceIR(constant.ref))
    }
}