package com.timepath.quakec.ast

import java.util.LinkedHashMap
import com.timepath.quakec.ast.impl.FunctionLiteral
import java.util.Stack
import com.timepath.quakec.ast.impl.FunctionCall
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.ast.impl.ConstantExpression
import com.timepath.quakec.ast.impl.BinaryExpression
import com.timepath.quakec.ast.impl.ReferenceExpression
import com.timepath.quakec.ast.impl.ReturnStatement
import com.timepath.quakec.ast.impl.DeclarationExpression
import java.util.HashMap


class GenerationContext(val root: BlockStatement) {

    val registry: Registry = Registry()

    data class Scope(val lookup: MutableMap<String, Int> = HashMap())

    inner class Registry {

        var counter: Int = 100
        val values: MutableMap<Int, Any> = HashMap()
        val reverse: MutableMap<Int, String> = LinkedHashMap()
        val scope = Stack<Scope>()

        inline fun all(operation: (Scope) -> Unit) = scope.reverse().forEach(operation)

        fun contains(name: String): Boolean {
            all {
                if (it.lookup.containsKey(name)) {
                    return true
                }
            }
            return false
        }

        fun get(name: String): Int? {
            all {
                val i = it.lookup[name]
                if (i != null) {
                    return i
                }
            }
            return null
        }

        fun register(name: String?, value: Any? = null): Int {
            val n = name ?: "var$counter"
            val existing = this[n]
            if (existing != null) return existing
            val i = counter++
            if (value != null)
                values[i] = value
            reverse[i] = n
            scope.peek().lookup[n] = i
            return i
        }

        override fun toString() = reverse.map { "${it.key}\t${it.value}\t${values[it.key]}" }.join("\n")

        fun push() {
            scope.push(Scope())
        }

        fun pop() {
            scope.pop()
        }

        {
            push()
        }

    }

    fun generate(): List<IR> {
        return root.generate()
    }

    private fun error(msg: String) {
        println("E: $msg")
    }

    private fun warn(msg: String) {
        println("W: $msg")
    }

    private fun Statement.enter() {
        println ("${"> > " repeat registry.scope.size()} ${this.javaClass.getSimpleName()}")
        when (this) {
            is BlockStatement -> {
                registry.push()
            }
            is DeclarationExpression -> {
            }
            is ReferenceExpression -> {
                if (id !in registry) {
                    error("unknown reference $id")
                }
            }
        }
    }

    private fun Statement.exit() {
        println ("${" < <" repeat registry.scope.size()} ${this.javaClass.getSimpleName()}")
        when (this) {
            is BlockStatement -> {
                registry.pop()
            }
            is FunctionLiteral -> {
                if (id != null && id in registry) {
                    warn("redefining $id")
                }
            }
        }
    }

    private fun Statement.generate(): List<IR> {
        this.enter()
        when (this) {
            is BlockStatement -> {
                return children.flatMap {
                    it.generate()
                }
            }
            is FunctionLiteral -> {
                val global = registry.register(id)
                return (children.flatMap { it.generate() }
                        + IR(ret = global, dummy = true))
            }
            is ConstantExpression -> {
                val global = registry.register(null, value)
                return listOf(
                        IR(ret = global, dummy = true))
            }
            is DeclarationExpression -> {
                val global = registry.register(id)
                return listOf(
                        IR(ret = global, dummy = true))
            }
            is ReferenceExpression -> {
                val global = registry[id]!!
                return listOf(
                        IR(ret = global, dummy = true))
            }
            is BinaryExpression.Assign -> {
                // ast:
                // left(a) = right(b)
                // vm:
                // b (=) a
                val genL = left.generate()
                val genR = right.generate()
                return (genL + genR
                        + IR(instr, array(genR.last().ret, genL.last().ret), genL.last().ret, this.toString()))
            }
            is BinaryExpression<*, *> -> {
                // ast:
                // temp(c) = left(a) op right(b)
                // vm:
                // c (=) a (op) b
                val genL = left.generate()
                val genR = right.generate()
                val global = registry.register(null)
                return (genL + genR
                        + IR(instr, array(genL.last().ret, genR.last().ret, global), global, this.toString()))
            }
            is FunctionCall -> {
                val args = args.map { it.generate() }
                val instr = {(i: Int) ->
                    Instruction.from(Instruction.CALL0.ordinal() + i)
                }
                var i = 0
                val prepare: List<IR> = args.map {
                    val param = Instruction.OFS_PARAM(i++)
                    IR(Instruction.STORE_FLOAT, array(it.last().ret, param), param, "Prepare param $i")
                }
                return (args.flatMap { it }
                        + prepare
                        + listOf(IR(instr(i), array(function!!.generate().last().ret), Instruction.OFS_PARAM(-1)))
                        )
            }
            is ReturnStatement -> {
                return listOf(
                        IR(Instruction.RETURN, array(0, 0, 0), 0))
            }
        }
        this.exit()
        return listOf()
    }

}