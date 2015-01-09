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
import java.util.regex.Pattern
import com.timepath.quakec.ast.impl.ConditionalExpression

class GenerationContext(val roots: List<Statement>) {

    val registry: Registry = Registry()

    data class Scope(val lookup: MutableMap<String, Int> = HashMap())

    inner class Registry {

        var counter: Int = 100
        val values: MutableMap<Int, Any> = HashMap()
        val reverse: MutableMap<Int, String> = LinkedHashMap()
        val scope = Stack<Scope>()

        inline fun all(operation: (Scope) -> Unit) = scope.reverse().forEach(operation)

        fun vecName(name: String): String? {
            val vec = Pattern.compile("(.+)_[xyz]$")
            val matcher = vec.matcher(name)
            if (matcher.matches()) {
                return matcher.group(1)
            }
            return null
        }

        fun contains(name: String): Boolean {
            all {
                if (it.lookup.containsKey(name)) {
                    return true
                }
                if (it.lookup.containsKey(vecName(name))) {
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
                val j = it.lookup[vecName(name)]
                if (j != null) {
                    return j
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
            register("_")
        }

    }

    fun generate(): List<IR> {
        return BlockStatement(roots).generate()
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
            is FunctionLiteral -> {
                if (id != null && id in registry) {
                    warn("redefining $id")
                }
            }
            is DeclarationExpression -> {
                // do nothing
            }
            is ReferenceExpression -> {
                if (id !in registry) {
                    error("unknown reference $id")
                }
            }
        }
    }

    private fun Statement.exit() {
        when (this) {
            is BlockStatement -> {
                registry.pop()
            }
            is FunctionLiteral -> {
                registry.pop()
            }
        }
        println ("${" < <" repeat registry.scope.size()} ${this.javaClass.getSimpleName()}")
    }

    private fun Statement.generate(): List<IR> {
        this.enter()
        val ret: List<IR> = when (this) {
            is BlockStatement -> {
                registry.push()
                children.flatMap {
                    it.generate()
                }
            }
            is FunctionLiteral -> {
                val global = registry.register(id)
                registry.push()
                (children.flatMap { it.generate() }
                        + IR(ret = global, dummy = true))
            }
            is ConstantExpression -> {
                val global = registry.register(null, value)
                listOf(
                        IR(ret = global, dummy = true))
            }
            is DeclarationExpression -> {
                val global = registry.register(id)
                listOf(
                        IR(ret = global, dummy = true))
            }
            is ReferenceExpression -> {
                val global = registry[id]!!
                listOf(IR(ret = global, dummy = true))
            }
            is BinaryExpression.Assign -> {
                // ast:
                // left(a) = right(b)
                // vm:
                // b (=) a
                val genL = left.generate()
                val genR = right.generate()
                (genL + genR
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
                (genL + genR
                        + IR(instr, array(genL.last().ret, genR.last().ret, global), global, this.toString()))
            }
            is ConditionalExpression -> {
                // TODO
                test.generate()
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
                (args.flatMap { it }
                        + prepare
                        + listOf(IR(instr(i), array(function!!.generate().last().ret), Instruction.OFS_PARAM(-1)))
                        )
            }
            is ReturnStatement -> {
                listOf(
                        IR(Instruction.RETURN, array(0, 0, 0), 0))
            }
            else -> emptyList()
        }
        this.exit()
        return ret
    }

}