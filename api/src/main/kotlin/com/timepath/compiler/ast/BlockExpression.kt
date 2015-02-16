package com.timepath.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR

class BlockExpression(add: List<Expression>? = null, ctx: ParserRuleContext? = null) : Expression(ctx) {
    {
        if (add != null) {
            addAll(add)
        }
    }

    override fun type(gen: Generator) = children.last().type(gen)

    override fun generate(gen: Generator): List<IR> {
        gen.allocator.push(this)
        val list = children.flatMap {
            it.doGenerate(gen)
        }
        gen.allocator.pop()
        return list
    }
}
