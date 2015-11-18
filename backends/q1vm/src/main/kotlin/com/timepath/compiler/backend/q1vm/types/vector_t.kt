package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.ir.IR
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.defaults.struct_t

// TODO: identify as value
object vector_t : struct_t("x" to float_t, "y" to float_t, "z" to float_t) {
    override val simpleName = "vector_t"
    override fun handle(op: Operation) = ops[op]
    override fun declare(name: String, value: ConstantExpression?)
            = DeclarationExpression(name, this, value)

    val ops = mapOf(
            Operation("=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[vector_t::class.java]),
            Operation("==", this, this) to DefaultHandlers.Binary(bool_t, Instruction.EQ[vector_t::class.java]),
            Operation("!=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.NE[vector_t::class.java]),
            Operation("+", this) to Operation.Handler.Unary(this) { it.generate() },
            Operation("!", this) to DefaultHandlers.Unary(bool_t, Instruction.NOT[vector_t::class.java]),
            Operation("*", this, float_t) to DefaultHandlers.Binary(this, Instruction.MUL_VEC_FLOAT),
            Operation("*", this, int_t) to DefaultHandlers.Binary(this, Instruction.MUL_VEC_FLOAT),
            Operation("*=", this, float_t) to DefaultHandlers.Assign(this, Instruction.STORE[vector_t::class.java]) { l, r -> l * r },
            Operation("*=", this, int_t) to DefaultHandlers.Assign(this, Instruction.STORE[vector_t::class.java]) { l, r -> l * r },
            Operation("/", this, float_t) to Operation.Handler.Binary(this) { l, r -> (l * (1.expr() / r)).generate() },
            Operation("/", this, int_t) to Operation.Handler.Binary(this) { l, r -> (l * (1.expr() / r)).generate() },
            Operation("/=", this, float_t) to DefaultHandlers.Assign(this, Instruction.STORE[vector_t::class.java]) { l, r -> l / r },
            Operation("+", this, this) to DefaultHandlers.Binary(this, Instruction.ADD_VEC),
            Operation("+=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[vector_t::class.java]) { l, r -> l + r },
            Operation("-", this, this) to DefaultHandlers.Binary(this, Instruction.SUB_VEC),
            Operation("-=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[vector_t::class.java]) { l, r -> l - r },
            Operation("*", this, this) to Operation.Handler.Binary(float_t) { l, r ->
                val x = vector_t["x"]
                val y = vector_t["y"]
                val z = vector_t["z"]
                (l[x] * r[x] + l[y] * r[y] + l[z] * r[z]).generate()
            },
            Operation("|=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[vector_t::class.java]) { l, r -> l or r },
            Operation("|", this, this) to Operation.Handler.Binary(this) { l, r ->
                linkedListOf<IR>().apply {
                    val ref = allocator.allocateReference(type = this@vector_t, scope = Instruction.Ref.Scope.Local)
                    val genL = l.generate()
                    addAll(genL)
                    val genR = r.generate()
                    addAll(genR)
                    repeat(3) { i ->
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        (component.set(
                                MemoryReference(genL.last().ret + i, type = float_t)
                                        or MemoryReference(genR.last().ret + i, type = float_t)))
                                .let { addAll(it.generate()) }
                    }
                }
            },
            Operation("&=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[vector_t::class.java]) { l, r -> l and r },
            Operation("&", this, this) to Operation.Handler.Binary(this) { l, r ->
                linkedListOf<IR>().apply {
                    val ref = allocator.allocateReference(type = this@vector_t, scope = Instruction.Ref.Scope.Local)
                    val gen = l.generate()
                    addAll(gen)
                    val genR = r.generate()
                    addAll(genR)
                    repeat(3) { i ->
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        component.set(
                                MemoryReference(gen.last().ret + i, type = float_t)
                                        and MemoryReference(genR.last().ret + i, type = float_t))
                                .let { addAll(it.generate()) }
                    }
                }
            },
            Operation("~", this) to Operation.Handler.Unary(this) {
                linkedListOf<IR>().apply {
                    val ref = allocator.allocateReference(type = this@vector_t, scope = Instruction.Ref.Scope.Local)
                    val gen = it.generate()
                    addAll(gen)
                    repeat(3) { i ->
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        component.set(MemoryReference(gen.last().ret + i, type = float_t).inv())
                                .let { addAll(it.generate()) }
                    }
                }
            },
            Operation("-", this) to Operation.Handler.Unary(this) {
                linkedListOf<IR>().apply {
                    val ref = allocator.allocateReference(type = this@vector_t, scope = Instruction.Ref.Scope.Local)
                    val gen = it.generate()
                    addAll(gen)
                    repeat(3) { i ->
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        component.set(-MemoryReference(gen.last().ret + i, type = float_t))
                                .let { addAll(it.generate()) }
                    }
                }
            }
    )
}
