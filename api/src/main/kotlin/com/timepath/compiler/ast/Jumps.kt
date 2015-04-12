package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext

// TODO: conditional goto
class GotoExpression(val id: String, override val ctx: ParserRuleContext? = null) : Expression() {
    override val simpleName = "GotoExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = "goto $id"

}

/**
 * Return can be assigned to, and has a constant address
 */
class ReturnStatement(val returnValue: Expression?, override val ctx: ParserRuleContext? = null) : Expression() {
    init {
        if (returnValue != null) {
            add(returnValue)
        }
    }
    override val simpleName = "ReturnStatement"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

}

// TODO: on labels
class ContinueStatement(override val ctx: ParserRuleContext? = null) : Expression() {
    override val simpleName = "ContinueStatement"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = "continue"

}

// TODO: on labels
class BreakStatement(override val ctx: ParserRuleContext? = null) : Expression() {
    override val simpleName = "BreakStatement"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString(): String = "break"

}
