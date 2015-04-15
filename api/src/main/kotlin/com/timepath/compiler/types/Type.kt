package com.timepath.compiler.types

import com.timepath.compiler.Named
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.Expression

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

abstract class Type : Named {

    override fun toString() = javaClass.getSimpleName().toLowerCase()

    abstract fun declare(name: String, value: ConstantExpression? = null, state: CompileState? = null): List<Expression>

    abstract fun handle(op: Operation): OperationHandler<*>?

}

data class Operation(val op: String, val left: Type, val right: Type? = null)

open class OperationHandler<T>(val type: Type, protected val handle: (state: CompileState, left: Expression, right: Expression?) -> T) {

    fun invoke(state: CompileState, left: Expression, right: Expression? = null): T = handle(state, left, right)
}
