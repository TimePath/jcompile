package com.timepath.quakec.compiler.api

import com.timepath.quakec.compiler.TypeRegistry
import com.timepath.quakec.compiler.ast.Expression
import org.antlr.v4.runtime.ANTLRInputStream

public trait Frontend {
    fun parse(stream: ANTLRInputStream, types: TypeRegistry): Expression
}
