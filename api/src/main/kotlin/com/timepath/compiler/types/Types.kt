package com.timepath.compiler.types

object Types {

    val types = hashMapOf<Class<*>, Type>()

    fun from(any: Any?): Type {
        val hashMap = types
        val type = hashMap[any?.javaClass]
        if (type != null) return type
        throw NoWhenBranchMatchedException()
    }

    fun type(operation: Operation) = handle<Any>(operation).type

    val handlers = linkedListOf<(Operation) -> OperationHandler<*>?>()

    fun <T> handle(operation: Operation): OperationHandler<T> {
        handlers.forEach {
            it(operation)?.let {
                (it as OperationHandler<T>).let { return it }
            }
        }
        throw UnsupportedOperationException("$operation")
    }
}
