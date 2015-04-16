package com.timepath.compiler.api

import com.timepath.compiler.ast.Expression

public trait Backend {
    val state: CompileState
    fun generate(roots: List<Expression>): Any

    object Null : Backend {
        override val state: CompileState get() = throw UnsupportedOperationException()
        override fun generate(roots: List<Expression>) = throw UnsupportedOperationException()
    }
}
