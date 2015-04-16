package com.timepath.compiler.ast

import com.timepath.compiler.data.Value
import org.antlr.v4.runtime.ParserRuleContext

class ConstantExpression(any: Any, override val ctx: ParserRuleContext? = null) : Expression() {
    val value = when (any) {
        is Value -> any : Value
        else -> Value(any)
    }

    override val simpleName = "ConstantExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = value.toString()

}
