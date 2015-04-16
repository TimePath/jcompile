package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.ConditionalExpression
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.backends.q1vm.DefaultAssignHandler
import com.timepath.compiler.backends.q1vm.Q1VM
import com.timepath.compiler.backends.q1vm.gen.generate
import com.timepath.q1vm.Instruction

object void_t : Type() {
    override val simpleName = "void_t"
    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("&&", this, this) to OperationHandler(bool_t) { gen: Q1VM.State, left, right ->
                // TODO: Instruction.AND when no side effects
                ConditionalExpression(left, true,
                        fail = ConstantExpression(0),
                        pass = ConditionalExpression(right!!, true,
                                fail = ConstantExpression(0),
                                pass = ConstantExpression(1))
                ).generate(gen)
            },
            // TODO: perl behaviour
            Operation("||", this, this) to OperationHandler(bool_t) { gen: Q1VM.State, left, right ->
                // TODO: Instruction.OR when no side effects
                ConditionalExpression(left, true,
                        pass = ConstantExpression(1),
                        fail = ConditionalExpression(right!!, true,
                                pass = ConstantExpression(1),
                                fail = ConstantExpression(0))
                ).generate(gen)
            },
            Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT)
    )

    override fun declare(name: String, value: ConstantExpression?, state: CompileState?): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}
