package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

/**
 * Lonely semicolon
 */
public class Nop(override val ctx: PRC? = null) : Expression() {
    override val simpleName = "Nop"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)
}
