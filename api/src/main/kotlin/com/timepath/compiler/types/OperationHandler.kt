package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.Expression

public open class OperationHandler<State : CompileState, T>(
        val type: Type,
        protected val handle: (state: State, left: Expression, right: Expression?) -> T
) {
    fun invoke(state: CompileState, left: Expression, right: Expression? = null): T {
        state as State // FIXME: Why do I have to assert this?
        return handle(state, left, right)
    }
}
