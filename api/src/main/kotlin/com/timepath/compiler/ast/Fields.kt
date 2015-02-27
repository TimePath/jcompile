package com.timepath.compiler.ast

import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

/**
 * dynamic:
 * array[index], entity.(field)
 */
// TODO: arrays
class IndexExpression(left: Expression, right: Expression, ctx: ParserRuleContext? = null) : BinaryExpression("[]", left, right, ctx) {
    override val simpleName = "IndexExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    var instr = Instruction.LOAD_FLOAT
}

/**
 * static:
 * struct.field
 */
// TODO: structs
class MemberExpression(left: Expression, val field: String, ctx: ParserRuleContext? = null) : BinaryExpression(".", left, ConstantExpression(field), ctx) {
    override val simpleName = "MemberExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

    var instr = Instruction.LOAD_FLOAT
}
