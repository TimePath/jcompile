package com.timepath.compiler.types

import com.timepath.q1vm.Instruction
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.api.CompileState

data class field_t(val type: Type) : pointer_t() {

    override fun toString() = ".$type"

    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FIELD),
            Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_FUNC),
            Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_FUNC)
    )

    override fun declare(name: String, value: ConstantExpression?, state: CompileState?): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}
