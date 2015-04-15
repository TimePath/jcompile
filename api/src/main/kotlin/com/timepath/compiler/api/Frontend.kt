package com.timepath.compiler.api

import com.timepath.compiler.ast.Expression
import org.antlr.v4.runtime.ANTLRInputStream

public trait Frontend {
    fun parse(stream: ANTLRInputStream, state: CompileState): Expression
}
