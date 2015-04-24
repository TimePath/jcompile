package com.timepath.compiler.frontend.kotlin

import com.timepath.compiler.Compiler
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.api.Frontend
import com.timepath.compiler.ast.Expression

public class Kotlin : Frontend<CompileState, Sequence<List<Expression>>> {
    override fun parse(includes: List<Compiler.Include>, state: CompileState) = throw UnsupportedOperationException()
    override fun define(name: String, value: String) = throw UnsupportedOperationException()
}
