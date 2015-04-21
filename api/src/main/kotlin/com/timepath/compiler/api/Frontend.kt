package com.timepath.compiler.api

import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.Expression
import java.util.LinkedList

public trait Frontend {
    public fun parse(includes: List<Compiler.Include>, state: CompileState): List<List<Expression>>
    public fun define(name: String, value: String)

    object Null : Frontend {
        override fun parse(includes: List<Compiler.Include>, state: CompileState): List<List<Expression>> = throw UnsupportedOperationException()
        override fun define(name: String, value: String) = throw UnsupportedOperationException()
    }
}
