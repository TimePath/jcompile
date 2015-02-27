package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext

class LabelExpression(val id: String, override val ctx: ParserRuleContext? = null) : Expression() {
    override val simpleName = "LabelExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = "$id:"

}

