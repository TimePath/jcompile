package com.timepath.compiler.ast

import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.compiler.Type
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

// TODO: conditional goto
class GotoExpression(val id: String, ctx: ParserRuleContext? = null) : Expression(ctx) {
    override fun type(gen: Generator) = throw UnsupportedOperationException()

    override val attributes: Map<String, Any?>
        get() = mapOf("label" to id)

    override fun toString(): String = "goto $id"

}

/**
 * Return can be assigned to, and has a constant address
 */
class ReturnStatement(val returnValue: Expression?, ctx: ParserRuleContext? = null) : Expression(ctx) {
    {
        if (returnValue != null) {
            add(returnValue)
        }
    }

    override fun type(gen: Generator) = returnValue?.type(gen) ?: Type.Void

}

// TODO: on labels
class ContinueStatement(ctx: ParserRuleContext? = null) : Expression(ctx) {
    override fun type(gen: Generator) = throw UnsupportedOperationException()

    override fun toString(): String = "continue"

}

// TODO: on labels
class BreakStatement(ctx: ParserRuleContext? = null) : Expression(ctx) {
    override fun type(gen: Generator) = throw UnsupportedOperationException()

    override fun toString(): String = "break"

}
