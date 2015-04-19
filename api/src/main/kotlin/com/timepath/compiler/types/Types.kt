package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState

public object Types {

    val types = hashMapOf<Class<*>, Type>()

    fun from(any: Any?): Type {
        val hashMap = types
        val type = hashMap[any?.javaClass]
        if (type != null) return type
        throw NoWhenBranchMatchedException()
    }

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
