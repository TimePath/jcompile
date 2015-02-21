package com.timepath.compiler.ast

import com.timepath.compiler.types.Type
import com.timepath.compiler.gen.Generator
import org.antlr.v4.runtime.ParserRuleContext

class SwitchExpression(val test: Expression, add: List<Expression>, override val ctx: ParserRuleContext? = null) : Expression() {

    {
        addAll(add)
    }

    class Case(
            /**
             * Case expression, null = default
             */
            val expr: Expression?,
            override val ctx: ParserRuleContext? = null) : Expression() {

    }

}
