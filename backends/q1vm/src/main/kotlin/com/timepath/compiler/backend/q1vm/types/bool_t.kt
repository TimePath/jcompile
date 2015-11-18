package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.ast.eq
import com.timepath.compiler.ast.expr
import com.timepath.compiler.ast.unaryMinus
import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.ir.IR
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Operation

object bool_t : number_t() {
    override val simpleName = "bool_t"
    val ops = mapOf(
            Operation("==", this, this) to DefaultHandlers.Binary(bool_t, Instruction.EQ[float_t::class.java]),
            Operation("!=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.NE[float_t::class.java]),
            Operation("!", this) to Operation.Handler.Unary(bool_t) {
                (0.expr() eq it).generate()
            },
            Operation("-", this) to Operation.Handler.Unary(this) { (-it).generate() },
            Operation("+", this, this) to DefaultHandlers.Binary(this, Instruction.ADD_FLOAT),
            Operation("-", this, this) to DefaultHandlers.Binary(this, Instruction.SUB_FLOAT),
            Operation("*", this, float_t) to DefaultHandlers.Binary(float_t, Instruction.MUL_FLOAT),
            Operation("*", this, int_t) to DefaultHandlers.Binary(float_t, Instruction.MUL_FLOAT),
            Operation("|", this, float_t) to DefaultHandlers.Binary(int_t, Instruction.BITOR),
            Operation("&", this, int_t) to DefaultHandlers.Binary(int_t, Instruction.BITAND),
            Operation("<=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.LE),
            Operation("<", this, this) to DefaultHandlers.Binary(bool_t, Instruction.LT),
            Operation(">=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.GE),
            Operation(">", this, this) to DefaultHandlers.Binary(bool_t, Instruction.GT)
    )

    override fun handle(op: Operation): Operation.Handler<Q1VM.State, List<IR>>? {
        ops[op]?.let {
            return it
        }
        // TODO: remove
        if (op.right != bool_t) {
            return ops[op.copy(right = bool_t)]
        }
        return null
    }
}
