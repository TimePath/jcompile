package com.timepath.quakec.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.compiler.gen.ReferenceIR
import com.timepath.quakec.compiler.Type

/**
 * dynamic:
 * array[index], entity.(field)
 */
class IndexExpression(left: Expression, right: Expression, ctx: ParserRuleContext? = null) : BinaryExpression<Expression, Expression>("[]", left, right, ctx) {

    var instr = Instruction.LOAD_FLOAT

    // TODO: arrays
    override fun generate(gen: Generator) = with(linkedListOf<IR>()) {
        val genL = left.doGenerate(gen)
        addAll(genL)
        val genR = right.doGenerate(gen)
        addAll(genR)
        val out = gen.allocator.allocateReference()
        add(IR(instr, array(genL.last().ret, genR.last().ret, out.ref), out.ref, this.toString()))
        this
    }
}

/**
 * static:
 * struct.field
 */
class MemberExpression(left: Expression, val field: String, ctx: ParserRuleContext? = null) : BinaryExpression<Expression, Expression>(".", left, ConstantExpression(field), ctx) {
    // TODO: structs
    override fun generate(gen: Generator) = Type.handle(Type.Operation(op, Type.Entity, Type.String))(gen, left, right)
}
