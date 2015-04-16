package com.timepath.compiler.api

import com.timepath.compiler.ast.DeclarationExpression

public trait SymbolTable {
    val globalScope: Boolean
    fun push(name: String)
    fun pop()
    fun declare<R>(e: R): R
    fun resolve(id: String): DeclarationExpression?
}
