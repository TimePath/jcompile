package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.ast.expr
import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.pointer_t
import com.timepath.q1vm.Instruction

data class field_t(val type: Type) : pointer_t() {

    override val simpleName = "field_t"
    override fun toString() = ".$type"

    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FIELD),
            Operation("==", this, this) to DefaultHandlers.Binary(bool_t, Instruction.EQ_FUNC),
            Operation("!=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.NE_FUNC)
    )

    override fun declare(name: String, value: ConstantExpression?, state: CompileState) =
            listOf(DeclarationExpression(name, this, value ?: 0.expr()))
}
