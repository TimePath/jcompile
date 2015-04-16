package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.Expression

open class OperationHandler<T>(val type: Type, protected val handle: (state: CompileState, left: Expression, right: Expression?) -> T) {

    fun invoke(state: CompileState, left: Expression, right: Expression? = null): T = handle(state, left, right)
}
