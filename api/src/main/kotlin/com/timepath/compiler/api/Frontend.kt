package com.timepath.compiler.api

import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.Expression

public trait Frontend<State : CompileState> {
    public fun parse(includes: List<Compiler.Include>, state: State): Sequence<List<Expression>>
    public fun define(name: String, value: String)

    object Null : Frontend<CompileState> {
        override fun parse(includes: List<Compiler.Include>, state: CompileState): Sequence<List<Expression>> = throw UnsupportedOperationException()
        override fun define(name: String, value: String) = throw UnsupportedOperationException()
    }
}
