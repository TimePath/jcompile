package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.Expression

public data class Operation(val op: String, val left: Type, val right: Type? = null) {
    public interface Handler<State : CompileState, T> {
        val type: Type

        companion object {
            fun <State : CompileState, T> Binary(type: Type,
                                                func: State.(lhs: Expression, rhs: Expression) -> T)
                    = object : Operation.Handler<State, T> {
                override val type = type

                override operator fun invoke(state: State, left: Expression, right: Expression?): T {
                    requireNotNull(right)
                    return state.func(left, right!!)
                }
            }

            fun <State : CompileState, T> Unary(type: Type,
                                               func: State.(it: Expression) -> T)
                    = object : Operation.Handler<State, T> {
                override val type = type

                override operator fun invoke(state: State, left: Expression, right: Expression?): T {
                    assert(right == null)
                    return state.func(left)
                }
            }
        }

        operator fun invoke(state: State, left: Expression, right: Expression? = null): T
    }

}
