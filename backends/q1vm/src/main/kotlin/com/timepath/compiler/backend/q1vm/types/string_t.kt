package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.defaults.pointer_t
import com.timepath.q1vm.Instruction

object string_t : pointer_t() {
    override val simpleName = "string_t"
    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_STR),
            Operation("==", this, this) to DefaultHandlers.Binary(bool_t, Instruction.EQ_STR),
            Operation("!=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.NE_STR),
            Operation("!", this) to DefaultHandlers.Unary(bool_t, Instruction.NOT_STR)
    )

    override fun declare(name: String, value: ConstantExpression?, state: CompileState): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}
