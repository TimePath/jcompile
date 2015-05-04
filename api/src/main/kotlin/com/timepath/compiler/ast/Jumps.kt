package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext as PRC

// TODO: conditional goto
public class GotoExpression(val id: String, override val ctx: PRC? = null) : Expression() {
    override val simpleName = "GotoExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = "goto ${id}"

}

/**
 * Return can be assigned to, and has a constant address
 */
public class ReturnStatement(val returnValue: Expression?, override val ctx: PRC? = null) : Expression() {
    init {
        returnValue?.let { add(it) }
    }

    override val simpleName = "ReturnStatement"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = "return ${returnValue}"
}

// TODO: on labels
public class ContinueStatement(override val ctx: PRC? = null) : Expression() {
    override val simpleName = "ContinueStatement"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = "continue"

}

// TODO: on labels
public class BreakStatement(override val ctx: PRC? = null) : Expression() {
    override val simpleName = "BreakStatement"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

    override fun toString() = "break"

}
