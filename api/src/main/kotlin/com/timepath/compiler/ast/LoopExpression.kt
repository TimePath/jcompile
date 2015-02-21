package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.gen.Generator
import org.antlr.v4.runtime.ParserRuleContext

class LoopExpression(val predicate: Expression,
                     body: Expression,
                     val checkBefore: Boolean = true,
                     val initializer: List<Expression>? = null,
                     val update: List<Expression>? = null,
                     override val ctx: ParserRuleContext? = null) : Expression() {
    {
        add(body)
    }

}
