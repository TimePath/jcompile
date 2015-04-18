package com.timepath.compiler.backend.q1vm.types

import com.timepath.Logger
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.gen.DefaultAssignHandler
import com.timepath.compiler.backend.q1vm.gen.DefaultHandler
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.pointer_t
import com.timepath.q1vm.Instruction

data class field_t(val type: Type) : pointer_t() {

    companion object {
        val logger = Logger.new()
    }

    override val simpleName = "field_t"
    override fun toString() = ".$type"

    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FIELD),
            Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_FUNC),
            Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_FUNC)
    )

    override fun declare(name: String, value: ConstantExpression?, state: CompileState?) = when {
        state!!.symbols.globalScope -> {
            state as Q1VM.State
            if (name in state.fields) {
                logger.warning("redeclaring field $name")
            }
            entity_t.fields[name] = this.type
            // TODO: namespace entity
            DeclarationExpression(name, this, state.fields[name])
        }
        else -> DeclarationExpression(name, this, value ?: ConstantExpression(0))
    }.let { listOf(it) }
}
