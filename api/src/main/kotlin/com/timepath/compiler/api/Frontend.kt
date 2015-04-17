package com.timepath.compiler.api

import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.Expression

public trait Frontend {
    public fun parse(include: Compiler.Include, state: CompileState): Expression
    public fun define(name: String, value: String)

    object Null : Frontend {
        override fun parse(include: Compiler.Include, state: CompileState): Expression = throw UnsupportedOperationException()
        override fun define(name: String, value: String) = throw UnsupportedOperationException()
    }
}
