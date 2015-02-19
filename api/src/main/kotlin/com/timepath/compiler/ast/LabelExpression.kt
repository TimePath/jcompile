package com.timepath.compiler.ast

import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.compiler.gen.LabelIR
import org.antlr.v4.runtime.ParserRuleContext

class LabelExpression(val id: String, ctx: ParserRuleContext? = null) : Expression(ctx) {
    override fun type(gen: Generator) = throw UnsupportedOperationException()

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to id)

    override fun toString(): String = "$id:"

}

