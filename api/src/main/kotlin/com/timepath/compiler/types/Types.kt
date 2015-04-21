package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState

public object Types {

    val types: MutableMap<Class<*>, Type> = hashMapOf()

    fun from(any: Any) = types[any.javaClass]!!

    fun type(operation: Operation) = handle<CompileState, Any>(operation).type

    val handlers = linkedListOf<(Operation) -> OperationHandler<*, *>?>()

    fun handle<S : CompileState, T>(operation: Operation): OperationHandler<S, T> {
        handlers.forEach {
            it(operation)?.let {
                (it as? OperationHandler<S, T>)?.let { return it }
            }
        }
        throw UnsupportedOperationException("$operation")
    }
}
