package com.timepath.compiler.types

import com.timepath.q1vm.Instruction
import com.timepath.compiler.ast.BinaryExpression
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.gen.generate
import com.timepath.compiler.ast.DeclarationExpression

object bool_t : number_t() {
    val ops: Map<Operation, OperationHandler>
        get() = mapOf(
                Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_FLOAT),
                Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_FLOAT),
                Operation("!", this) to OperationHandler(bool_t) { gen, self, _ ->
                    BinaryExpression.Eq(ConstantExpression(0f), self).generate(gen)
                },
                Operation("-", this) to OperationHandler(this) { gen, self, _ ->
                    BinaryExpression.Subtract(ConstantExpression(0f), self).generate(gen)
                },
                Operation("+", this, this) to DefaultHandler(this, Instruction.ADD_FLOAT),
                Operation("-", this, this) to DefaultHandler(this, Instruction.SUB_FLOAT),
                Operation("*", this, float_t) to DefaultHandler(float_t, Instruction.MUL_FLOAT),
                Operation("*", this, int_t) to DefaultHandler(float_t, Instruction.MUL_FLOAT),
                Operation("|", this, float_t) to DefaultHandler(int_t, Instruction.BITOR),
                Operation("&", this, int_t) to DefaultHandler(int_t, Instruction.BITAND),
                Operation("<=", this, this) to DefaultHandler(bool_t, Instruction.LE),
                Operation("<", this, this) to DefaultHandler(bool_t, Instruction.LT),
                Operation(">=", this, this) to DefaultHandler(bool_t, Instruction.GE),
                Operation(">", this, this) to DefaultHandler(bool_t, Instruction.GT)
        )

    override fun handle(op: Operation): OperationHandler? {
        ops[op]?.let {
            return it
        }
        // TODO: remove
        if (op.right != bool_t) {
            return ops[op.copy(right = bool_t)]
        }
        return null
    }

    override fun declare(name: String, value: ConstantExpression?): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}
