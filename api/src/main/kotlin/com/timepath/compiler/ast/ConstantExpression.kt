package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.Value
import com.timepath.compiler.gen.Generator
import org.antlr.v4.runtime.ParserRuleContext

class ConstantExpression(any: Any, override val ctx: ParserRuleContext? = null) : Expression() {
    val value = Value(any)

    override fun toString(): String = value.toString()

}
