package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.backend.q1vm.IR
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.OperationHandler
import com.timepath.compiler.types.defaults.struct_t
import com.timepath.q1vm.Instruction

// TODO: identify as value
object vector_t : struct_t("x" to float_t, "y" to float_t, "z" to float_t) {
    override val simpleName = "vector_t"
    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_VEC),
            Operation("==", this, this) to DefaultHandlers.Binary(bool_t, Instruction.EQ_VEC),
            Operation("!=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.NE_VEC),
            Operation("+", this) to OperationHandler.Unary(this) { it.generate() },
            Operation("!", this) to DefaultHandlers.Unary(bool_t, Instruction.NOT_VEC),
            Operation("*", this, float_t) to DefaultHandlers.Binary(this, Instruction.MUL_VEC_FLOAT),
            Operation("*", this, int_t) to DefaultHandlers.Binary(this, Instruction.MUL_VEC_FLOAT),
            Operation("*=", this, float_t) to DefaultHandlers.Assign(this, Instruction.STORE_VEC) { l, r -> l * r },
            Operation("*=", this, int_t) to DefaultHandlers.Assign(this, Instruction.STORE_VEC) { l, r -> l * r },
            Operation("/", this, float_t) to OperationHandler.Binary(this) { l, r -> (l * (ConstantExpression(1) / r)).generate() },
            Operation("/", this, int_t) to OperationHandler.Binary(this) { left, right -> (left * (ConstantExpression(1) / right)).generate() },
            Operation("/=", this, float_t) to DefaultHandlers.Assign(this, Instruction.STORE_VEC) { l, r -> l / r },
            Operation("+", this, this) to DefaultHandlers.Binary(this, Instruction.ADD_VEC),
            Operation("+=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_VEC) { l, r -> l + r },
            Operation("-", this, this) to DefaultHandlers.Binary(this, Instruction.SUB_VEC),
            Operation("-=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_VEC) { l, r -> l - r },
            Operation("*", this, this) to OperationHandler.Binary(float_t) { l, r ->
                val x = vector_t["x"]
                val y = vector_t["y"]
                val z = vector_t["z"]
                (l[x] * r[x] + l[y] * r[y] + l[z] * r[z]).generate()
            },
            Operation("|=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_VEC) { l, r -> l or r },
            Operation("|", this, this) to OperationHandler.Binary(this) { l, r ->
                with(linkedListOf<IR>()) {
                    val ref = allocator.allocateReference(type = this@vector_t)
                    val genL = l.generate()
                    addAll(genL)
                    val genR = r.generate()
                    addAll(genR)
                    for (i in 0..3 - 1) {
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        BinaryExpression.Assign(
                                component,
                                MemoryReference(genL.last().ret + i, type = float_t)
                                        or MemoryReference(genR.last().ret + i, type = float_t)
                        ).let { addAll(it.generate()) }
                    }
                    this
                }
            },
            Operation("&=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_VEC) { l, r -> l and r },
            Operation("&", this, this) to OperationHandler.Binary(this) { l, r ->
                with(linkedListOf<IR>()) {
                    val ref = allocator.allocateReference(type = this@vector_t)
                    val gen = l.generate()
                    addAll(gen)
                    val genR = r.generate()
                    addAll(genR)
                    for (i in 0..3 - 1) {
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        BinaryExpression.Assign(component,
                                MemoryReference(gen.last().ret + i, type = float_t) and
                                        MemoryReference(genR.last().ret + i, type = float_t))
                                .let { addAll(it.generate()) }
                    }
                    this
                }
            },
            Operation("~", this) to OperationHandler.Unary(this) {
                with(linkedListOf<IR>()) {
                    val ref = allocator.allocateReference(type = this@vector_t)
                    val gen = it.generate()
                    addAll(gen)
                    for (i in 0..3 - 1) {
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        BinaryExpression.Assign(component, MemoryReference(gen.last().ret + i, type = float_t).inv())
                                .let { addAll(it.generate()) }
                    }
                    this
                }
            },
            Operation("-", this) to OperationHandler.Unary(this) {
                with(linkedListOf<IR>()) {
                    val ref = allocator.allocateReference(type = this@vector_t)
                    val gen = it.generate()
                    addAll(gen)
                    for (i in 0..3 - 1) {
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        BinaryExpression.Assign(component, -MemoryReference(gen.last().ret + i, type = float_t))
                                .let { addAll(it.generate()) }
                    }
                    this
                }
            }
    )
}
