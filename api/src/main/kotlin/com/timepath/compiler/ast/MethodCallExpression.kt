package com.timepath.compiler.ast

import kotlin.properties.Delegates
import com.timepath.compiler.Type
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

class MethodCallExpression(val function: Expression,
                           add: List<Expression>? = null,
                           ctx: ParserRuleContext? = null) : Expression(ctx) {

    {
        if (add != null) {
            addAll(add)
        }
    }

    override fun type(gen: Generator): Type = (function.type(gen) as Type.Function).type

    val args: List<Expression> by Delegates.lazy {
        children.filterIsInstance<Expression>()
    }

    override fun toString(): String = "$function(${args.joinToString(", ")})"

}
