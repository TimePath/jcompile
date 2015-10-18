package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t
import com.timepath.compiler.types.defaults.pointer_t

data class field_t(val type: Type) : pointer_t() {

    override val simpleName = "field_t"
    override fun toString() = ".$type"

    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[field_t::class.java]),
            Operation("==", this, this) to DefaultHandlers.Binary(bool_t, Instruction.EQ[function_t::class.java]),
            Operation("!=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.NE[function_t::class.java])
    )
}
