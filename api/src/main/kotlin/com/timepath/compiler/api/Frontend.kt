package com.timepath.compiler.api

import com.timepath.compiler.Compiler

public interface Frontend<State : CompileState, O : Any> {
    public fun parse(includes: List<Compiler.Include>, state: State): O
    public fun define(name: String, value: String = "1")
}
