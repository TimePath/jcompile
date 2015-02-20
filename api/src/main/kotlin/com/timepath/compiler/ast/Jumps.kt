package com.timepath.compiler.ast

import com.timepath.compiler.gen.Generator
import com.timepath.compiler.Type
import org.antlr.v4.runtime.ParserRuleContext

// TODO: conditional goto
class GotoExpression(val id: String, override val ctx: ParserRuleContext? = null) : Expression() {
    override fun type(gen: Generator) = Type.Void

    override fun toString(): String = "goto $id"

}

/**
 * Return can be assigned to, and has a constant address
 */
class ReturnStatement(val returnValue: Expression?, override val ctx: ParserRuleContext? = null) : Expression() {
    {
        if (returnValue != null) {
            add(returnValue)
        }
    }

    override fun type(gen: Generator) = returnValue?.type(gen) ?: Type.Void

}

// TODO: on labels
class ContinueStatement(override val ctx: ParserRuleContext? = null) : Expression() {
    override fun type(gen: Generator) = Type.Void

    override fun toString(): String = "continue"

}

// TODO: on labels
class BreakStatement(override val ctx: ParserRuleContext? = null) : Expression() {
    override fun type(gen: Generator) = Type.Void

    override fun toString(): String = "break"

}
