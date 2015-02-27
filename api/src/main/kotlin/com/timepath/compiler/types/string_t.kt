package com.timepath.compiler.types

import com.timepath.q1vm.Instruction
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.api.CompileState

object string_t : pointer_t() {
    override val simpleName = "string_t"
    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_STR),
            Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_STR),
            Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_STR),
            Operation("!", this) to DefaultUnaryHandler(bool_t, Instruction.NOT_STR)
    )

    override fun declare(name: String, value: ConstantExpression?, state: CompileState?): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}
