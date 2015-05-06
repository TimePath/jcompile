package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.Expression

public trait OperationHandler<State : CompileState, T> {
    val type: Type

    companion object {
        inline fun Binary<State : CompileState, T>(type: Type,
                                                   inlineOptions(InlineOption.ONLY_LOCAL_RETURN)
                                                   func: State.(lhs: Expression, rhs: Expression) -> T)
                = object : OperationHandler<State, T> {
            override val type = type

            override fun invoke(state: State, left: Expression, right: Expression?): T {
                requireNotNull(right)
                return state.func(left, right!!)
            }
        }

        inline fun Unary<State : CompileState, T>(type: Type,
                                                  inlineOptions(InlineOption.ONLY_LOCAL_RETURN)
                                                  func: State.(it: Expression) -> T)
                = object : OperationHandler<State, T> {
            override val type = type

            override fun invoke(state: State, left: Expression, right: Expression?): T {
                assert(right == null)
                return state.func(left)
            }
        }
    }

    fun invoke(state: State, left: Expression, right: Expression? = null): T
}
