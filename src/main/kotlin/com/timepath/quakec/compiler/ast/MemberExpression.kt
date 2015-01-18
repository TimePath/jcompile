package com.timepath.quakec.compiler.ast

import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.vm.Instruction

class MemberExpression(left: Expression, right: Expression, ctx: ParserRuleContext? = null) : BinaryExpression<Expression, Expression>(left, right, ctx) {
    override var instr = Instruction.LOAD_FLOAT
    override val op = "."
}