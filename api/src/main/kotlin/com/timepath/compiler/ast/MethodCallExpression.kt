package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

public class MethodCallExpression(val function: Expression,
                                  add: List<Expression>? = null,
                                  override val ctx: PRC? = null) : Expression() {

    init {
        add?.let { addAll(it) }
    }

    override val simpleName = "MethodCallExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    val args: List<Expression> by lazy(LazyThreadSafetyMode.NONE) {
        children.filterIsInstance<Expression>()
    }

    override fun toString(): String = "$function(${args.joinToString(", ")})"

}
