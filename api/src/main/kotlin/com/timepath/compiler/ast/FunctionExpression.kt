package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.gen.Generator
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Replaced with a number during compilation
 */
class FunctionExpression(val id: String? = null,
                         val signature: Type.Function,
                         val params: List<Expression>? = null,
                         val vararg: Expression? = null,
                         add: List<Expression>? = null,
                         val builtin: Int? = null,
                         override val ctx: ParserRuleContext? = null) : Expression() {

    {
        if (add != null) {
            addAll(add)
        }
    }

    override fun type(gen: Generator) = signature

}
