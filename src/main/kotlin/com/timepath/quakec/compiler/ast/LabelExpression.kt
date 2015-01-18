package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.CaseIR
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.LabelIR
import org.antlr.v4.runtime.ParserRuleContext

class LabelExpression(val id: String, ctx: ParserRuleContext? = null) : Expression(ctx) {
    override val attributes: Map<String, Any?>
        get() = mapOf("id" to id)

    override fun generate(ctx: Generator): List<IR> {
        return listOf(LabelIR(id))
    }
}

