package com.timepath.compiler.api

public trait Backend<State : CompileState, I : Any, O : Any> {
    val state: State
    fun compile(roots: I): O
}
