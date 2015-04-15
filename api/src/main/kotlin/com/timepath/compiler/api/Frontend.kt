package com.timepath.compiler.api

import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.Expression
import org.anarres.cpp.Source
import org.antlr.v4.runtime.ANTLRInputStream

public trait Frontend {
    fun parse(include: Compiler.Include, state: CompileState): Expression

    fun define(name: String, value: String)
}
