package com.timepath.compiler.types.defaults

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.OperationHandler
import com.timepath.compiler.types.Type

public data class function_t(val type: Type, val argTypes: List<Type>, val vararg: Type? = null) : pointer_t() {

    override val simpleName = "function_t"
    override fun toString() = "($argTypes${when (vararg) {
        null -> ""
        else -> ", $vararg..."
    }}) -> $type"

    override fun handle(op: Operation): OperationHandler<*, *>? {
        handlers.forEach {
            it(op)?.let { return it }
        }
        return null
    }

    companion object {

        val handlers = linkedListOf<function_t.(Operation) -> OperationHandler<*, *>?>()

        var ops = hashMapOf<Operation, OperationHandler<*, *>>()
    }

    override fun declare(name: String, value: ConstantExpression?, state: CompileState?): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}
