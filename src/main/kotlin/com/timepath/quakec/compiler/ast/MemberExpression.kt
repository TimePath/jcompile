package com.timepath.quakec.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR

class MemberExpression(left: Expression, right: Expression, ctx: ParserRuleContext? = null) : BinaryExpression<Expression, Expression>(".", left, right, ctx) {

    var instr = Instruction.LOAD_FLOAT

    override fun generate(ctx: Generator) = with(linkedListOf<IR>()) {
        val genL = left.doGenerate(ctx)
        addAll(genL)
        val genR = right.doGenerate(ctx)
        addAll(genR)
        val out = ctx.allocator.allocateReference()
        add(IR(instr, array(genL.last().ret, genR.last().ret, out.ref), out.ref, this.toString()))
        this
    }
}