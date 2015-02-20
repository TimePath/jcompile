package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.compiler.Type

class BlockExpression(add: List<Expression>? = null, override val ctx: ParserRuleContext? = null) : Expression() {
    {
        if (add != null) {
            addAll(add)
        }
    }

    override fun type(gen: Generator): Type {
        return Type.Void
        // TODO: return children.last().type(gen)
    }

}
