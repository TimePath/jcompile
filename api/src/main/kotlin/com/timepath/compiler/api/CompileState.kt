package com.timepath.compiler.api

import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.AliasExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.types.Type
import java.util.*

public abstract class CompileState {

    val errors: MutableList<Compiler.Err> = linkedListOf()

    val types = object : TypeRegistry {

        private val types: MutableMap<String, Type> = linkedMapOf()

        override operator fun get(name: String) = types[name]

        override operator fun set(name: String, t: Type) {
            types[name] = t
        }
    }

    val symbols = object : SymbolTable {

        inner data class Scope(val name: String, val vars: MutableMap<String, DeclarationExpression> = linkedMapOf())

        private val stack: Deque<Scope> = linkedListOf()

        override val insideFunc: Boolean
            get() = stack.size() >= 3

        override fun push(name: String) = stack.push(Scope(name))

        override fun pop() {
            stack.pop()
        }

        override fun declare<R>(e: R): R {
            val top = stack.peek()
            when (e) {
                is AliasExpression -> top.vars[e.id] = e.alias
                is DeclarationExpression -> top.vars[e.id] = e
            }
            return e
        }

        override operator fun get(id: String) = stack.firstOrNull { id in it.vars }?.let { it.vars[id] }
    }

}
