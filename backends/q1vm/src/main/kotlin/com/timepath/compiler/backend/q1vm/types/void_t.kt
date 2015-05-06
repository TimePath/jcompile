package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.ConditionalExpression
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.OperationHandler
import com.timepath.compiler.types.Type
import com.timepath.q1vm.Instruction

object void_t : Type() {
    override val simpleName = "void_t"
    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("&&", this, this) to OperationHandler.Binary(bool_t) { l, r ->
                // TODO: Instruction.AND when no side effects
                ConditionalExpression(l, true,
                        fail = ConstantExpression(0),
                        pass = ConditionalExpression(r, true,
                                fail = ConstantExpression(0),
                                pass = ConstantExpression(1))
                ).generate()
            },
            // TODO: perl behaviour
            Operation("||", this, this) to OperationHandler.Binary(bool_t) { l, r ->
                // TODO: Instruction.OR when no side effects
                ConditionalExpression(l, true,
                        pass = ConstantExpression(1),
                        fail = ConditionalExpression(r, true,
                                pass = ConstantExpression(1),
                                fail = ConstantExpression(0))
                ).generate()
            },
            Operation("=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT)
    )

    override fun declare(name: String, value: ConstantExpression?, state: CompileState): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}
