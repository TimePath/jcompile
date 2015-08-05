package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.defaults.pointer_t

object string_t : pointer_t() {
    override val simpleName = "string_t"
    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[javaClass<string_t>()]),
            Operation("==", this, this) to DefaultHandlers.Binary(bool_t, Instruction.EQ[javaClass<string_t>()]),
            Operation("!=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.NE[javaClass<string_t>()]),
            Operation("!", this) to DefaultHandlers.Unary(bool_t, Instruction.NOT[javaClass<string_t>()])
    )
}
