package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.gen.Generator
import org.antlr.v4.runtime.ParserRuleContext

class SwitchExpression(val test: Expression, add: List<Expression>, override val ctx: ParserRuleContext? = null) : Expression() {

    override fun type(gen: Generator) = test.type(gen)

            ;{
        addAll(add)
    }

    class Case(
            /**
             * Case expression, null = default
             */
            val expr: Expression?,
            override val ctx: ParserRuleContext? = null) : Expression() {
        override fun type(gen: Generator) = expr?.type(gen) ?: Type.Void

    }

}
