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
 *
 * TODO: arrays
 */
class IndexExpression(left: Expression, right: Expression, ctx: ParserRuleContext? = null) : BinaryExpression<Expression, Expression>("[]", left, right, ctx) {

    var instr = Instruction.LOAD_FLOAT

    override fun generate(gen: Generator) = with(linkedListOf<IR>()) {
        val genL = left.doGenerate(gen)
        addAll(genL)
        val genR = right.doGenerate(gen)
        addAll(genR)
        val out = gen.allocator.allocateReference(type = type(gen))
        add(IR(instr, array(genL.last().ret, genR.last().ret, out.ref), out.ref, this.toString()))
        this
    }
}

/**
 * static:
 * struct.field
 *
 * TODO: structs
 */
class MemberExpression(left: Expression, val field: String, ctx: ParserRuleContext? = null) : BinaryExpression<Expression, Expression>(".", left, ConstantExpression(field), ctx) {

    var instr = Instruction.LOAD_FLOAT

    // TODO: based on field
    override fun type(gen: Generator) = Type.Float

    override fun handler(gen: Generator) = Type.handle(Type.Operation(op, Type.Entity, Type.String))

    override fun generate(gen: Generator) = with(linkedListOf<IR>()) {
        val genL = left.doGenerate(gen)
        addAll(genL)
        val genR = ConstantExpression(0).doGenerate(gen) // TODO: field by name
        addAll(genR)
        val out = gen.allocator.allocateReference(type = type(gen))
        add(IR(instr, array(genL.last().ret, genR.last().ret, out.ref), out.ref, this.toString()))
        this
    }

}
