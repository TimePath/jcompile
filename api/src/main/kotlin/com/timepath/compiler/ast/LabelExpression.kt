package com.timepath.compiler.ast

import com.timepath.compiler.gen.Generator
import org.antlr.v4.runtime.ParserRuleContext

class LabelExpression(val id: String, override val ctx: ParserRuleContext? = null) : Expression() {
    override fun type(gen: Generator) = throw UnsupportedOperationException()

    override fun toString(): String = "$id:"

}

