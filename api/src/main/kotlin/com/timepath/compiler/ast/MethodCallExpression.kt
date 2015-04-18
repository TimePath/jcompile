package com.timepath.compiler.ast

import kotlin.properties.Delegates
import org.antlr.v4.runtime.ParserRuleContext as PRC

public class MethodCallExpression(val function: Expression,
                                  add: List<Expression>? = null,
                                  override val ctx: PRC? = null) : Expression() {

    init {
        add?.let { addAll(it) }
    }

    override val simpleName = "MethodCallExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    val args: List<Expression> by Delegates.lazy {
        children.filterIsInstance<Expression>()
    }

    override fun toString(): String = "$function(${args.joinToString(", ")})"

}
