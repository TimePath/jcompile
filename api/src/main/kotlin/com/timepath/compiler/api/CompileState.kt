package com.timepath.compiler.api

import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.types.Type
import java.util.Deque
import java.util.LinkedHashMap
import java.util.LinkedList

public abstract class CompileState {

    val types = object : TypeRegistry {

        private val types = linkedMapOf<String, Type>()

        override fun get(name: String): Type {
            return types[name]
        }

        override fun set(name: String, t: Type) {
            types[name] = t
        }
    }

    val symbols = object : SymbolTable {

        inner data class Scope(val name: String, val vars: MutableMap<String, DeclarationExpression> = LinkedHashMap())

        private val stack: Deque<Scope> = LinkedList()

        override val globalScope: Boolean get() = stack.size() < 3

        override fun push(name: String) = stack.push(Scope(name))

        override fun pop() {
            stack.pop()
        }

        override fun declare<R>(e: R): R {
            val vars = stack.peek().vars
            if (e is DeclarationExpression) {
                vars[e.id] = e
            }
            return e
        }

        override fun resolve(id: String) = stack.firstOrNull { id in it.vars }?.vars?.get(id)
    }

}
