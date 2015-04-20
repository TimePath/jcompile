package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.BinaryExpression
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.backend.q1vm.DefaultHandler
import com.timepath.compiler.backend.q1vm.IR
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.generate
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.OperationHandler
import com.timepath.q1vm.Instruction

object bool_t : number_t() {
    override val simpleName = "bool_t"
    val ops = mapOf(
            Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_FLOAT),
            Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_FLOAT),
            Operation("!", this) to OperationHandler(bool_t) { gen: Q1VM.State, self, _ ->
                BinaryExpression.Eq(ConstantExpression(0), self).generate(gen)
            },
            Operation("-", this) to OperationHandler(this) { gen: Q1VM.State, self, _ ->
                BinaryExpression.Subtract(ConstantExpression(0), self).generate(gen)
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

    override fun handle(op: Operation): OperationHandler<Q1VM.State, List<IR>>? {
        ops[op]?.let {
            return it
        }
        // TODO: remove
        if (op.right != bool_t) {
            return ops[op.copy(right = bool_t)]
        }
        return null
    }

    override fun declare(name: String, value: ConstantExpression?, state: CompileState): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}
