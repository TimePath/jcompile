package com.timepath.compiler.ast

import com.timepath.compiler.Value
import org.antlr.v4.runtime.ParserRuleContext as PRC

fun Any.expr() = ConstantExpression(this, null)

public class ConstantExpression(any: Any, override val ctx: PRC?) : Expression() {
    val value = if (any is Value) any else Value(any)

    override val simpleName = "ConstantExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = value.toString()

}
