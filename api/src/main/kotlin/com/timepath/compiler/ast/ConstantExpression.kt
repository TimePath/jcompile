package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.Value
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.compiler.gen.ReferenceIR
import org.antlr.v4.runtime.ParserRuleContext

class ConstantExpression(any: Any, ctx: ParserRuleContext? = null) : Expression(ctx) {
    val value = Value(any)

    override fun type(gen: Generator) = Type.from(value.value)

    override val attributes: Map<String, Any?>
        get() = mapOf("value" to value)

    override fun evaluate(): Value = value

    override fun toString(): String = value.toString()

}
