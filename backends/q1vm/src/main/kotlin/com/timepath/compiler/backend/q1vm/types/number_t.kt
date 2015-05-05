package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.backend.q1vm.IR
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.data.Pointer
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.OperationHandler
import com.timepath.compiler.types.Type
import com.timepath.q1vm.Instruction
import kotlin.properties.Delegates

open class number_t : Type() {
    override val simpleName = "number_t"
    override fun handle(op: Operation) = ops[op]
    private val ops by Delegates.lazy {
        mapOf(
                Operation("=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT),
                Operation("+", this, this) to DefaultHandlers.Binary(this, Instruction.ADD_FLOAT),
                Operation("-", this, this) to DefaultHandlers.Binary(this, Instruction.SUB_FLOAT),
                Operation("&", this) to OperationHandler.Unary(this) {
                    BinaryExpression.Divide(it, ConstantExpression(Pointer(1))).generate()
                },
                Operation("*", this) to OperationHandler.Unary(this) {
                    BinaryExpression.Multiply(it, ConstantExpression(Pointer(1))).generate()
                },
                Operation("+", this) to OperationHandler.Unary(this) {
                    it.generate()
                },
                Operation("-", this) to OperationHandler.Unary(this) {
                    BinaryExpression.Subtract(ConstantExpression(0), it).generate()
                },

                Operation("*", this, this) to DefaultHandlers.Binary(this, Instruction.MUL_FLOAT),
                Operation("*", this, vector_t) to DefaultHandlers.Binary(vector_t, Instruction.MUL_FLOAT_VEC),
                Operation("/", this, this) to DefaultHandlers.Binary(this, Instruction.DIV_FLOAT),
                Operation("%", this, this) to OperationHandler.Binary(this) { left, right ->
                    MethodCallExpression(DynamicReferenceExpression("__builtin_mod"), listOf(left, right)).generate()
                },

                // pre
                Operation("++", this) to OperationHandler.Unary(this) {
                    val one = UnaryExpression.Cast(int_t, ConstantExpression(1))
                    BinaryExpression.Assign(it, BinaryExpression.Add(it, one)).generate()
                },
                // post
                Operation("++", this, this) to OperationHandler.Unary(this) {
                    val one = UnaryExpression.Cast(int_t, ConstantExpression(1))
                    with(linkedListOf<IR>()) {
                        val add = BinaryExpression.Add(it, one)
                        val assign = BinaryExpression.Assign(it, add)
                        // FIXME
                        val sub = BinaryExpression.Subtract(assign, one)
                        addAll(sub.generate())
                        this
                    }
                },
                // pre
                Operation("--", this) to OperationHandler.Unary(this) {
                    val one = UnaryExpression.Cast(int_t, ConstantExpression(1))
                    BinaryExpression.Assign(it, BinaryExpression.Subtract(it, one)).generate()
                },
                // post
                Operation("--", this, this) to OperationHandler.Unary(this) {
                    val one = UnaryExpression.Cast(int_t, ConstantExpression(1))
                    with(linkedListOf<IR>()) {
                        val sub = BinaryExpression.Subtract(it, one)
                        val assign = BinaryExpression.Assign(it, sub)
                        // FIXME
                        val add = BinaryExpression.Add(assign, one)
                        addAll(add.generate())
                        this
                    }
                },

                Operation("==", this, this) to DefaultHandlers.Binary(bool_t, Instruction.EQ_FLOAT),
                Operation("!=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.NE_FLOAT),
                Operation(">", this, this) to DefaultHandlers.Binary(bool_t, Instruction.GT),
                Operation("<", this, this) to DefaultHandlers.Binary(bool_t, Instruction.LT),
                Operation(">=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.GE),
                Operation("<=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.LE),

                Operation("!", this) to OperationHandler.Unary(bool_t) {
                    BinaryExpression.Eq(ConstantExpression(0f), it).generate()
                },
                Operation("~", this) to OperationHandler.Unary(this) {
                    BinaryExpression.Subtract(ConstantExpression(-1), it).generate()
                },
                Operation("&", this, this) to DefaultHandlers.Binary(this, Instruction.BITAND),
                Operation("|", this, this) to DefaultHandlers.Binary(this, Instruction.BITOR),
                Operation("^", this, this) to OperationHandler.Binary(this) { left, right ->
                    MethodCallExpression(DynamicReferenceExpression("__builtin_xor"), listOf(left, right)).generate()
                },
                // TODO
                Operation("<<", this, this) to DefaultHandlers.Binary(int_t, Instruction.BITOR),
                // TODO
                Operation(">>", this, this) to DefaultHandlers.Binary(int_t, Instruction.BITOR),
                // TODO
                Operation("**", this, this) to DefaultHandlers.Binary(float_t, Instruction.BITOR),
                Operation("+=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Add(left, right)
                },
                Operation("-=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Subtract(left, right)
                },
                Operation("*=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Multiply(left, right)
                },
                Operation("/=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Divide(left, right)
                },
                Operation("%=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Modulo(left, right)
                },
                Operation("&=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.And(left, right)
                },
                Operation("|=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Or(left, right)
                },
                Operation("^=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.BitXor(left, right)
                },
                Operation("<<=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Lsh(left, right)
                },
                Operation(">>=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE_FLOAT) { left, right ->
                    BinaryExpression.Rsh(left, right)
                }
        )
    }

    override fun declare(name: String, value: ConstantExpression?, state: CompileState): List<DeclarationExpression> {
        return listOf(DeclarationExpression(name, this, value))
    }
}

object int_t : number_t() {
    override val simpleName = "int_t"
    override fun handle(op: Operation): OperationHandler<Q1VM.State, List<IR>>? {
        super.handle(op)?.let { return it }
        if (op.right == float_t) {
            return float_t.handle(op.copy(left = float_t))
        }
        if (op.right == bool_t) {
            return super.handle(op.copy(right = int_t))
        }
        return null
    }
}

object float_t : number_t() {
    override val simpleName = "float_t"
    override fun handle(op: Operation): OperationHandler<Q1VM.State, List<IR>>? {
        super.handle(op)?.let { return it }
        if (op.right == int_t || op.right == bool_t) {
            return super.handle(op.copy(right = float_t))
        }
        return null
    }
}
