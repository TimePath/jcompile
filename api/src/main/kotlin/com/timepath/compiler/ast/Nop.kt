package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext

/**
 * Lonely semicolon
 */
class Nop(override val ctx: ParserRuleContext? = null) : Expression() {
    override val simpleName = "Nop"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
}
