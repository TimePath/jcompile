package com.timepath.quakec.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR

class BlockExpression(add: List<Expression>? = null, ctx: ParserRuleContext? = null) : Expression(ctx) {
    {
        if (add != null) {
            addAll(add)
        }
    }
    override fun generate(ctx: Generator): List<IR> {
        ctx.allocator.push("<block>")
        val list = children.flatMap {
            it.doGenerate(ctx)
        }
        ctx.allocator.pop()
        return list
    }
}