package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

public class BlockExpression(add: List<Expression>? = null, override val ctx: PRC? = null) : Expression() {
    init {
        add?.let { addAll(it) }
    }

    override val simpleName = "BlockExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)
}
