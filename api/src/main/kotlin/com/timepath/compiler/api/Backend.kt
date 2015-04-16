package com.timepath.compiler.api

import com.timepath.compiler.ast.Expression

public trait Backend<State : CompileState> {
    val state: State
    fun generate(roots: List<Expression>): Any

    object Null : Backend<CompileState> {
        override val state: CompileState get() = throw UnsupportedOperationException()
        override fun generate(roots: List<Expression>) = throw UnsupportedOperationException()
    }
}
