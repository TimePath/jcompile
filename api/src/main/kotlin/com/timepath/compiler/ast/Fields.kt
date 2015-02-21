package com.timepath.compiler.ast

import com.timepath.compiler.gen.Generator
import com.timepath.compiler.types.Type
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.compiler.gen.type
import com.timepath.compiler.types.string_t
import com.timepath.compiler.types.Operation

/**
 * dynamic:
 * array[index], entity.(field)
 */
// TODO: arrays
class IndexExpression(left: Expression, right: Expression, ctx: ParserRuleContext? = null) : BinaryExpression("[]", left, right, ctx) {

    var instr = Instruction.LOAD_FLOAT
}

/**
 * static:
 * struct.field
 */
// TODO: structs
class MemberExpression(left: Expression, val field: String, ctx: ParserRuleContext? = null) : BinaryExpression(".", left, ConstantExpression(field), ctx) {

    var instr = Instruction.LOAD_FLOAT
}
