package com.timepath.quakec.ast

import java.util.LinkedHashMap
import com.timepath.quakec.ast.impl.BlockStatement
import java.util.LinkedList
import com.timepath.quakec.ast.impl.BinaryExpression
import com.timepath.quakec.ast.impl.ReferenceExpression
import com.timepath.quakec.ast.impl.DeclarationExpression
import com.timepath.quakec.ast.impl.FunctionCall
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.ast.impl.FunctionLiteral
import com.timepath.quakec.ast.impl.ReturnStatement
import com.timepath.quakec.ast.impl.ConstantExpression


class GenerationContext(val root: BlockStatement) {

    val registry: Registry = Registry()

    inner class Registry {

        var counter: Int = 100
        val values: MutableMap<Int, Any> = LinkedHashMap()
        val reverse: MutableMap<Int, String> = LinkedHashMap()
        val lookup: MutableMap<String, Int> = LinkedHashMap()

        fun contains(name: String): Boolean = lookup.containsKey(name)

        fun get(name: String): Int = lookup[name] ?: 0

        fun register(name: String?, value: Any? = null): Int {
            val n = name ?: "var$counter"
            val existing = lookup[n]
            if (existing != null) return existing
            val i = counter++
            if (value != null)
                values[i] = value
            reverse[i] = n
            lookup[n] = i
            return i
        }

        override fun toString() = reverse.map { "${it.key}\t${it.value}\t${values[it.key]}" }.join("\n")

    }

    fun generate(): List<IR> {
        return root.generate(this)
    }

    private fun Statement.generate(ctx: GenerationContext): List<IR> {
        when (this) {
            is BlockStatement -> {
                return children.flatMap { it.generate(ctx) }
            }
            is BinaryExpression.Assign -> {
                // left = right
                val genL = left.generate(ctx)
                val genR = right.generate(ctx)
                return genL + genR + IR(instr, array(genR.last().ret, genL.last().ret), genL.last().ret, "=")
            }
            is BinaryExpression<*, *> -> {
                val genL = left.generate(ctx)
                val genR = right.generate(ctx)
                val allocate = ctx.registry.register(null)
                return genL + genR + IR(instr, array(genL.last().ret, genR.last().ret, allocate), allocate, "${left} $op ${right}")

            }
            is DeclarationExpression -> {
//                if (super.generate(ctx).isNotEmpty()) return super.generate(ctx)
                val global = ctx.registry.register(this.id)
                return listOf(IR(ret = global, dummy = true))
            }
            is ReferenceExpression -> {
                if (id in ctx.registry)
                    return listOf(IR(ret = ctx.registry[id], dummy = true))
            }
            is FunctionCall -> {
                val args = args.map { it.generate(ctx) }
                val instr = {(i: Int) ->
                    Instruction.from(Instruction.CALL0.ordinal() + i)
                }
                var i = 0
                val prepare: List<IR> = args.map {
                    val param = Instruction.OFS_PARAM(i)
                    IR(Instruction.STORE_FLOAT, array(it.last().ret, param), param)
                }
                return (args.flatMap { it }
                        + prepare
                        + listOf(IR(instr(i), array(function!!.generate(ctx).last().ret), Instruction.OFS_PARAM(-1)))
                        )
            }
            is FunctionLiteral -> {
//                    if (name!! in ctx.registry) return super.generate(ctx)
                    val global = ctx.registry.register(name)
                    return (block!!.generate(ctx) + IR(ret = global, dummy = true))
            }
            is ReturnStatement -> {
                return listOf(IR(Instruction.RETURN, array(0, 0, 0), 0))
            }
            is ConstantExpression -> {
                return listOf(IR(ret = ctx.registry.register(null, value), dummy = true))
            }
        }
        return listOf()
    }

}