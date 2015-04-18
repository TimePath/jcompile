package com.timepath.compiler.ast

import com.timepath.compiler.Value
import org.antlr.v4.runtime.ParserRuleContext as PRC

public class ConstantExpression(any: Any, override val ctx: PRC? = null) : Expression() {
    val value = when (any) {
        is Value -> any : Value
        else -> Value(any)
    }

    override val simpleName = "ConstantExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = value.toString()

}
