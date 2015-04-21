package com.timepath.compiler.api

import com.timepath.compiler.ast.Expression

public trait Backend<State : CompileState, O : Any> {
    val state: State
    fun generate(roots: Sequence<List<Expression>>): O

    object Null : Backend<CompileState, Any> {
        override val state: CompileState get() = throw UnsupportedOperationException()
        override fun generate(roots: Sequence<List<Expression>>) = throw UnsupportedOperationException()
    }
}
