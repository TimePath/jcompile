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
    override fun generate(gen: Generator): List<IR> {
        gen.allocator.push("<block>")
        val list = children.flatMap {
            it.doGenerate(gen)
        }
        gen.allocator.pop()
        return list
    }
}
