package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState
import java.lang.reflect.ParameterizedType

public object Types {

    val types: MutableMap<Class<*>, Type> = linkedMapOf()

    fun from(any: Any) = types[any.javaClass]!!

    fun type(operation: Operation) = handle<CompileState, Any>(operation).type

    val handlers = linkedListOf<(Operation) -> OperationHandler<*, *>?>()

    fun Class<*>.typeArguments() = (getGenericSuperclass() as? ParameterizedType)?.getActualTypeArguments()

    val debug = false

    suppress("UNCHECKED_CAST")
    inline fun handle<reified S : CompileState, reified T>(operation: Operation): OperationHandler<S, T> {
        for (handler in handlers) {
            val it = handler(operation)
            if (it == null) continue
            if (debug) {
                val types = it.javaClass.typeArguments()
                if (types == null) throw NullPointerException("OperationHandler has no RTTI")
                if (types[0] !is S) continue
                if (types[1] !is T) continue
            }
            it as OperationHandler<S, T>
            return it
        }
        throw UnsupportedOperationException("$operation")
    }
}
