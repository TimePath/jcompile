package com.timepath.compiler.api

import com.timepath.compiler.Pointer
import com.timepath.compiler.types.TypeRegistry
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.types.*
import java.util.Deque
import java.util.LinkedHashMap
import java.util.LinkedList

abstract class CompileState {

    val types = TypeRegistry()

    trait FieldCounter {
        fun get(name: String): ConstantExpression
        fun contains(name: String): Boolean
    }

    val fields = object : FieldCounter {
        val map = LinkedHashMap<String, Int>()
        override fun get(name: String) = ConstantExpression(Pointer(map.getOrPut(name) { map.size() }))
        override fun contains(name: String) = name in map
    }

    trait SymbolTable {
        val globalScope: Boolean
        fun push(name: String)
        fun pop()
        fun <R> declare(e: R): R
        fun resolve(id: String): DeclarationExpression?
    }

    val symbols = object : SymbolTable {

        inner data class Scope(val name: String, val vars: MutableMap<String, DeclarationExpression> = LinkedHashMap())

        private val stack: Deque<Scope> = LinkedList()

        override val globalScope: Boolean get() = stack.size() < 3

        override fun push(name: String) = stack.push(Scope(name))

        override fun pop() {
            stack.pop()
        }

        override fun <R> declare(e: R): R {
            val vars = stack.peek().vars
            if (e is DeclarationExpression) {
                vars[e.id] = e
            }
            return e
        }

        override fun resolve(id: String) = stack.firstOrNull { id in it.vars }?.vars?.get(id)
    }

}
