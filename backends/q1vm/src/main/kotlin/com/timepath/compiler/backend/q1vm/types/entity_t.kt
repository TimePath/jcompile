package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.defaults.struct_t

abstract class class_t : struct_t() {
    fun ops(self: struct_t) = mapOf(
            Operation("=", self, self) to DefaultHandlers.Assign(self, Instruction.STORE[javaClass<entity_t>()]),
            Operation("==", self, self) to DefaultHandlers.Binary(bool_t, Instruction.EQ[javaClass<entity_t>()]),
            Operation("!=", self, self) to DefaultHandlers.Binary(bool_t, Instruction.NE[javaClass<entity_t>()]),
            Operation("!", self) to DefaultHandlers.Unary(bool_t, Instruction.NOT[javaClass<entity_t>()])
    )

    override fun declare(name: String, value: ConstantExpression?)
            = DeclarationExpression(name, this, value)

    override fun sizeOf(): Int = 1

    fun extend(name: String): class_t = object : class_t() {
        override val simpleName = name
        override fun handle(op: Operation) = ops[op] ?: throw UnsupportedOperationException("$op")
        val ops = ops(this)
    }
}

object entity_t : class_t() {
    override val simpleName = "entity_t"
    override fun handle(op: Operation) = ops[op]
    val ops = ops(this)
}
