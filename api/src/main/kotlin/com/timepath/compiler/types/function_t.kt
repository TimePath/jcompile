package com.timepath.compiler.types

import com.timepath.q1vm.Instruction
import com.timepath.compiler.ast.BinaryExpression
import com.timepath.compiler.ast.MemoryReference
import com.timepath.compiler.gen.generate
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.api.CompileState

data class function_t(val type: Type, val argTypes: List<Type>, val vararg: Type? = null) : pointer_t() {

    override fun toString() = "($argTypes${when (vararg) {
        null -> ""
        else -> ", $vararg..."
    }}) -> $type"

    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FUNC),
            Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_FUNC),
            Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_FUNC),
            Operation("!", this) to DefaultUnaryHandler(bool_t, Instruction.NOT_FUNC),
            Operation("&", this) to OperationHandler(float_t) { gen, self, _ ->
                BinaryExpression.Divide(MemoryReference(self.generate(gen).last().ret, float_t), ConstantExpression(1)).generate(gen)
            }
    )

    override fun declare(name: String, value: ConstantExpression?, state: CompileState?): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}
