package com.timepath.compiler.api

import com.timepath.compiler.ast.DeclarationExpression

public interface SymbolTable {
    val insideFunc: Boolean
    fun push(name: String)
    fun pop()
    fun <R> declare(e: R): R
    operator fun get(id: String): DeclarationExpression?
    /** TODO: replace with isConst */
    fun isGlobal(id: String): Boolean
}
