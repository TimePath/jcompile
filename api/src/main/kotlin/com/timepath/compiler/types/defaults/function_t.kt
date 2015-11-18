package com.timepath.compiler.types.defaults

import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Type

public class function_t(val type: Type, val argTypes: List<Type>, val vararg: Type? = null) : pointer_t() {

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as function_t

        if (type != other.type) return false
        if (argTypes != other.argTypes) return false
        if (vararg != other.vararg) return false
        if (simpleName != other.simpleName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result += 31 * result + argTypes.hashCode()
        result += 31 * result + (vararg?.hashCode() ?: 0)
        result += 31 * result + simpleName.hashCode()
        return result
    }

    companion object {

        val handlers = linkedListOf<function_t.(Operation) -> Operation.Handler<*, *>?>()

    }
}
