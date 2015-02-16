package com.timepath.compiler.ast

import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.compiler.Type
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

/**
 * dynamic:
 * array[index], entity.(field)
 */
// TODO: arrays
class IndexExpression(left: Expression, right: Expression, ctx: ParserRuleContext? = null) : BinaryExpression<Expression, Expression>("[]", left, right, ctx) {

    var instr = Instruction.LOAD_FLOAT

    override fun type(gen: Generator): Type {
        val typeL = left.type(gen)
        return if (typeL is Type.Entity) {
            (right.type(gen) as Type.Field).type
        } else {
            super.type(gen)
        }
    }

    override fun generate(gen: Generator) = with(linkedListOf<IR>()) {
        val typeL = left.type(gen)
        if (typeL is Type.Entity) {
            val genL = left.doGenerate(gen)
            addAll(genL)
            val genR = right.doGenerate(gen)
            addAll(genR)
            val out = gen.allocator.allocateReference(type = type(gen))
            add(IR(instr, array(genL.last().ret, genR.last().ret, out.ref), out.ref, this.toString()))
            this
        } else {
            super.generate(gen)
        }
    }
}

/**
 * static:
 * struct.field
 */
// TODO: structs
class MemberExpression(left: Expression, val field: String, ctx: ParserRuleContext? = null) : BinaryExpression<Expression, Expression>(".", left, ConstantExpression(field), ctx) {

    var instr = Instruction.LOAD_FLOAT

    override fun type(gen: Generator): Type {
        val lhs = left.type(gen)
        return when (lhs) {
            is Type.Entity -> {
                // TODO: field namespace
                val rhs = ReferenceExpression(field).type(gen)
                (rhs as Type.Field).type
            }
            is Type.Struct -> {
                // TODO: struct member return type
                Type.Float
            }
        // TODO: vec_[xyz]
        //            else -> throw UnsupportedOperationException("field access on type $lhs")
            else -> lhs
        }
    }

    override fun handler(gen: Generator) = Type.handle(Type.Operation(op, left.type(gen), Type.String))

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