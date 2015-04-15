package com.timepath.compiler.types

import com.timepath.Logger
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
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
