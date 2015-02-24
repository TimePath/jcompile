package com.timepath.compiler.ast

import com.timepath.compiler.Value
import org.antlr.v4.runtime.ParserRuleContext

class ConstantExpression(any: Any, override val ctx: ParserRuleContext? = null) : Expression() {
    val value = when (any) {
        is Value -> any : Value
        else -> Value(any)
    }

    override fun toString(): String = value.toString()

}
