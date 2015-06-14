package com.timepath.compiler.api

public interface Backend<State : CompileState, I : Any, O : Any> {
    val state: State
    fun compile(roots: I): O
}
