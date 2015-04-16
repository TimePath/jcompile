package com.timepath.compiler.types

import com.timepath.compiler.ast.*
import com.timepath.compiler.backends.q1vm.DefaultAssignHandler
import com.timepath.compiler.backends.q1vm.DefaultHandler
import com.timepath.compiler.backends.q1vm.DefaultUnaryHandler
import com.timepath.compiler.backends.q1vm.gen.IR
import com.timepath.compiler.backends.q1vm.gen.generate
import com.timepath.compiler.types.defaults.struct_t
import com.timepath.q1vm.Instruction

// TODO: identify as value
object vector_t : struct_t("x" to float_t, "y" to float_t, "z" to float_t) {
    override val simpleName = "vector_t"
    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_VEC),
            Operation("==", this, this) to DefaultHandler(bool_t, Instruction.EQ_VEC),
            Operation("!=", this, this) to DefaultHandler(bool_t, Instruction.NE_VEC),
            Operation("+", this) to OperationHandler(this) { gen, self, _ ->
                self.generate(gen)
            },
            Operation("!", this) to DefaultUnaryHandler(bool_t, Instruction.NOT_VEC),
            Operation("*", this, float_t) to DefaultHandler(this, Instruction.MUL_VEC_FLOAT),
            Operation("*", this, int_t) to DefaultHandler(this, Instruction.MUL_VEC_FLOAT),
            Operation("*=", this, float_t) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                BinaryExpression.Multiply(left, right)
            },
            Operation("*=", this, int_t) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                BinaryExpression.Multiply(left, right)
            },
            Operation("/", this, float_t) to OperationHandler(this) { gen, left, right ->
                BinaryExpression.Multiply(left, BinaryExpression.Divide(ConstantExpression(1), right!!)).generate(gen)
            },
            Operation("/", this, int_t) to OperationHandler(this) { gen, left, right ->
                BinaryExpression.Multiply(left, BinaryExpression.Divide(ConstantExpression(1), right!!)).generate(gen)
            },
            Operation("/=", this, float_t) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                BinaryExpression.Divide(left, right)
            },
            Operation("+", this, this) to DefaultHandler(this, Instruction.ADD_VEC),
            Operation("+=", this, this) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                BinaryExpression.Add(left, right)
            },
            Operation("-", this, this) to DefaultHandler(this, Instruction.SUB_VEC),
            Operation("-=", this, this) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                BinaryExpression.Subtract(left, right)
            },
            Operation("*", this, this) to OperationHandler(float_t) { gen, left, right ->
                BinaryExpression.Add(BinaryExpression.Add(
                        BinaryExpression.Multiply(MemberExpression(left, "x"), MemberExpression(left, "x")),
                        BinaryExpression.Multiply(MemberExpression(left, "y"), MemberExpression(left, "y"))),
                        BinaryExpression.Multiply(MemberExpression(left, "z"), MemberExpression(left, "z"))
                ).generate(gen)
            },
            Operation("|", this, this) to OperationHandler(this) { gen, left, right ->
                with(linkedListOf<IR>()) {
                    val ref = gen.allocator.allocateReference(type = this@vector_t)
                    val genL = left.generate(gen)
                    addAll(genL)
                    val genR = right!!.generate(gen)
                    addAll(genR)
                    val lhs = genL.last()
                    val rhs = genR.last()
                    for (i in 0..2) {
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        addAll(BinaryExpression.Assign(component, BinaryExpression.BitOr(
                                MemoryReference(lhs.ret + i, type = float_t),
                                MemoryReference(rhs.ret + i, type = float_t)
                        )).generate(gen))
                    }
                    this
                }
            },
            Operation("|=", this, this) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                BinaryExpression.BitOr(left, right)
            },
            Operation("&", this, this) to OperationHandler(this) { gen, left, right ->
                with(linkedListOf<IR>()) {
                    val ref = gen.allocator.allocateReference(type = this@vector_t)
                    val genL = left.generate(gen)
                    addAll(genL)
                    val genR = right!!.generate(gen)
                    addAll(genR)
                    val lhs = genL.last()
                    val rhs = genR.last()
                    for (i in 0..2) {
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        addAll(BinaryExpression.Assign(component, BinaryExpression.BitAnd(
                                MemoryReference(lhs.ret + i, type = float_t),
                                MemoryReference(rhs.ret + i, type = float_t)
                        )).generate(gen))
                    }
                    this
                }
            },
            Operation("&=", this, this) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                BinaryExpression.BitAnd(left, right)
            },
            Operation("~", this) to OperationHandler(this) { gen, self, _ ->
                with(linkedListOf<IR>()) {
                    val ref = gen.allocator.allocateReference(type = this@vector_t)
                    val genL = self.generate(gen)
                    addAll(genL)
                    val lhs = genL.last()
                    for (i in 0..2) {
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        addAll(BinaryExpression.Assign(component, UnaryExpression.BitNot(
                                MemoryReference(lhs.ret + i, type = float_t)
                        )).generate(gen))
                    }
                    this
                }
            },
            Operation("-", this) to OperationHandler(this) { gen, self, _ ->
                with(linkedListOf<IR>()) {
                    val ref = gen.allocator.allocateReference(type = this@vector_t)
                    val genL = self.generate(gen)
                    addAll(genL)
                    val lhs = genL.last()
                    for (i in 0..2) {
                        val component = MemoryReference(ref.ref + i, type = float_t)
                        addAll(BinaryExpression.Assign(component, UnaryExpression.Minus(
                                MemoryReference(lhs.ret + i, type = float_t)
                        )).generate(gen))
                    }
                    this
                }
            }
    )
}
