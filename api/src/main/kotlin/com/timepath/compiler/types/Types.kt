package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState
import java.lang.reflect.ParameterizedType

public object Types {

    val types: MutableMap<Class<*>, Type> = linkedMapOf()

    fun from(any: Any) = types[any.javaClass]!!

    fun type(operation: Operation) = handle<CompileState, Any>(operation).type

    val handlers = linkedListOf<(Operation) -> Operation.Handler<*, *>?>()

    fun Class<*>.typeArguments() = (genericSuperclass as? ParameterizedType)?.actualTypeArguments

    val debug = false

    @Suppress("UNCHECKED_CAST")
    inline fun <reified S : CompileState, reified T> handle(operation: Operation): Operation.Handler<S, T> {
        for (handler in handlers) {
            val it = handler(operation) ?: continue
            if (debug) {
                val types = it.javaClass.typeArguments() ?: throw NullPointerException("Operation.Handler has no RTTI")
                if (types[0] !is S) continue
                if (types[1] !is T) continue
            }
            it as Operation.Handler<S, T>
            return it
        }
        throw UnsupportedOperationException("$operation")
    }
}
