package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.backend.q1vm.DefaultAssignHandler
import com.timepath.compiler.backend.q1vm.DefaultHandler
import com.timepath.compiler.backend.q1vm.DefaultUnaryHandler
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.defaults.struct_t
import com.timepath.q1vm.Instruction

abstract class class_t : struct_t() {
    fun ops(self: struct_t) = mapOf(
            Operation("=", self, self) to DefaultAssignHandler(self, Instruction.STORE_ENT),
            Operation("==", self, self) to DefaultHandler(bool_t, Instruction.EQ_ENT),
            Operation("!=", self, self) to DefaultHandler(bool_t, Instruction.NE_ENT),
            Operation("!", self) to DefaultUnaryHandler(bool_t, Instruction.NOT_ENT)
    )

    override fun declare(name: String, value: ConstantExpression?, state: CompileState): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }

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
