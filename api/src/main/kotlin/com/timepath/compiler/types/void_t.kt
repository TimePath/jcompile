package com.timepath.compiler.types

import com.timepath.compiler.ast.ConditionalExpression
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.gen.generate
import com.timepath.q1vm.Instruction
import com.timepath.compiler.ast.DeclarationExpression

object void_t : Type {
    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("&&", this, this) to OperationHandler(bool_t) { gen, left, right ->
                // TODO: Instruction.AND when no side effects
                ConditionalExpression(left, true,
                        fail = ConstantExpression(0),
                        pass = ConditionalExpression(right!!, true,
                                fail = ConstantExpression(0),
                                pass = ConstantExpression(1f))
                ).generate(gen)
            },
            // TODO: perl behaviour
            Operation("||", this, this) to OperationHandler(bool_t) { gen, left, right ->
                // TODO: Instruction.OR when no side effects
                ConditionalExpression(left, true,
                        pass = ConstantExpression(1f),
                        fail = ConditionalExpression(right!!, true,
                                pass = ConstantExpression(1f),
                                fail = ConstantExpression(0))
                ).generate(gen)
            },
            Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT)
    )

    override fun declare(name: String, value: ConstantExpression?): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}
