package com.timepath.compiler.ast

import com.timepath.compiler.Value
import com.timepath.compiler.types.Type
import org.antlr.v4.runtime.ParserRuleContext as PRC

fun Value.expr(name: String? = null) = ConstantExpression(this, name, null, null)
fun Boolean.expr(name: String? = null) = ConstantExpression(Value(this), name, null, null)
fun Int.expr(name: String? = null) = ConstantExpression(Value(this), name, null, null)
fun Float.expr(name: String? = null) = ConstantExpression(Value(this), name, null, null)

public class ConstantExpression(val value: Value, val name: String? = null, val type: Type? = null, override val ctx: PRC?) : Expression() {

    override val simpleName = "ConstantExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = value.toString()

}
