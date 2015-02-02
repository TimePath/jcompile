package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.CaseIR
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.LabelIR
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.compiler.Type

class LabelExpression(val id: String, ctx: ParserRuleContext? = null) : Expression(ctx) {
    override fun type(gen: Generator) = throw UnsupportedOperationException()

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to id)

    override fun toString(): String = "$id:"

    override fun generate(gen: Generator): List<IR> {
        return listOf(LabelIR(id))
    }
}

