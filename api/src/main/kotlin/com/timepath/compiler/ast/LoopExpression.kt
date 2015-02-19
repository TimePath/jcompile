package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

class LoopExpression(val predicate: Expression,
                     body: Expression,
                     val checkBefore: Boolean = true,
                     val initializer: List<Expression>? = null,
                     val update: List<Expression>? = null,
                     ctx: ParserRuleContext? = null) : Expression(ctx) {
    {
        add(body)
    }

    override fun type(gen: Generator) = Type.Void

}
