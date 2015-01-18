package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.CaseIR
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.LabelIR
import org.antlr.v4.runtime.ParserRuleContext

class Label(val id: String, ctx: ParserRuleContext? = null) : Statement(ctx) {
    override val attributes: Map<String, Any?>
        get() = mapOf("id" to id)

    override fun generate(ctx: Generator): List<IR> {
        return listOf(LabelIR(id))
    }
}

class CaseLabel(
        /**
         * Case expression, null = default
         */
        val expr: Expression?,
        ctx: ParserRuleContext? = null) : Statement(ctx) {

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to expr)

    override fun generate(ctx: Generator): List<IR> {
        return listOf(CaseIR(expr))
    }
}