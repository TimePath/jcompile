package com.timepath.compiler.types.defaults

import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Type

public data class function_t(val type: Type, val argTypes: List<Type>, val vararg: Type? = null) : pointer_t() {

    override val simpleName = "function_t"
    override fun toString() = "(${argTypes.joinToString(", ")}${when (vararg) {
        null -> ""
        else -> ", $vararg..."
    }}) -> $type"

    override fun handle(op: Operation): Operation.Handler<*, *>? {
        handlers.forEach {
            it(op)?.let { return it }
        }
        return null
    }

    companion object {

        val handlers = linkedListOf<function_t.(Operation) -> Operation.Handler<*, *>?>()

    }
}
