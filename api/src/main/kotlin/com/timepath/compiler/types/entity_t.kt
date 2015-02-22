package com.timepath.compiler.types

import com.timepath.q1vm.Instruction
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.gen.generate
import com.timepath.compiler.ast.MemoryReference
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.api.CompileState

object entity_t : pointer_t() {
    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_ENT),
            Operation(".", this, string_t) to OperationHandler(this) { gen, left, right ->
                // TODO: names to fields
                ConstantExpression(0).generate(gen)
            },
            Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_ENT),
            Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_ENT),
            Operation("!", this) to DefaultUnaryHandler(bool_t, Instruction.NOT_ENT),
            Operation("(int)", this) to OperationHandler(int_t) { gen, self, _ ->
                MemoryReference(self.generate(gen).last().ret, int_t).generate(gen)
            }
    )

    override fun declare(name: String, value: ConstantExpression?, state: CompileState?): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}