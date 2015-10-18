package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.DefaultHandlers
import com.timepath.compiler.backend.q1vm.Pointer
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.expr
import com.timepath.compiler.ir.IR
import com.timepath.compiler.ir.Instruction
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Type

open class number_t : Type() {
    override val simpleName = "number_t"
    override fun handle(op: Operation) = ops[op]
    private val ops by lazy(LazyThreadSafetyMode.NONE) {
        mapOf(
                Operation("=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java]),
                Operation("+", this, this) to DefaultHandlers.Binary(this, Instruction.ADD_FLOAT),
                Operation("-", this, this) to DefaultHandlers.Binary(this, Instruction.SUB_FLOAT),
                Operation("&", this) to Operation.Handler.Unary(this) { (it / Pointer(1).expr()).generate() },
                Operation("*", this) to Operation.Handler.Unary(this) { (it * Pointer(1).expr()).generate() },
                Operation("+", this) to Operation.Handler.Unary(this) { it.generate() },
                Operation("-", this) to Operation.Handler.Unary(this) { (0.expr() - it).generate() },
                Operation("*", this, this) to DefaultHandlers.Binary(this, Instruction.MUL_FLOAT),
                Operation("*", this, vector_t) to DefaultHandlers.Binary(vector_t, Instruction.MUL_FLOAT_VEC),
                Operation("/", this, this) to DefaultHandlers.Binary(this, Instruction.DIV_FLOAT),
                Operation("%", this, this) to Operation.Handler.Binary(this) { l, r ->
                    MethodCallExpression(symbols["__builtin_mod"]!!.ref(), listOf(l, r)).generate()
                },

                // pre
                Operation("++", this) to Operation.Handler.Unary(this) {
                    val one = 1.expr().to(int_t)
                    (it.set(it + one)).generate()
                },
                // post
                Operation("++", this, this) to Operation.Handler.Unary(this) {
                    val one = 1.expr().to(int_t)
                    linkedListOf<IR>() apply {
                        val assign = it.set(it + one)
                        // FIXME
                        addAll((assign - one).generate())
                    }
                },
                // pre
                Operation("--", this) to Operation.Handler.Unary(this) {
                    val one = 1.expr().to(int_t)
                    (it.set(it - one)).generate()
                },
                // post
                Operation("--", this, this) to Operation.Handler.Unary(this) {
                    val one = 1.expr().to(int_t)
                    linkedListOf<IR>() apply {
                        val assign = it.set(it - one)
                        // FIXME
                        addAll((assign + one).generate())
                    }
                },

                Operation("==", this, this) to DefaultHandlers.Binary(bool_t, Instruction.EQ[float_t::class.java]),
                Operation("!=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.NE[float_t::class.java]),
                Operation(">", this, this) to DefaultHandlers.Binary(bool_t, Instruction.GT),
                Operation("<", this, this) to DefaultHandlers.Binary(bool_t, Instruction.LT),
                Operation(">=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.GE),
                Operation("<=", this, this) to DefaultHandlers.Binary(bool_t, Instruction.LE),

                Operation("!", this) to Operation.Handler.Unary(bool_t) {
                    (0f.expr() eq it).generate()
                },
                Operation("~", this) to Operation.Handler.Unary(this) { ((-1).expr() - it).generate() },
                Operation("&", this, this) to DefaultHandlers.Binary(this, Instruction.BITAND),
                Operation("|", this, this) to DefaultHandlers.Binary(this, Instruction.BITOR),
                Operation("^", this, this) to Operation.Handler.Binary(this) { left, right ->
                    MethodCallExpression(symbols["__builtin_xor"]!!.ref(), listOf(left, right)).generate()
                },
                // TODO
                Operation("<<", this, this) to DefaultHandlers.Binary(int_t, Instruction.BITOR),
                // TODO
                Operation(">>", this, this) to DefaultHandlers.Binary(int_t, Instruction.BITOR),
                // TODO
                Operation("**", this, this) to DefaultHandlers.Binary(float_t, Instruction.BITOR),
                Operation("+=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java])
                { l, r -> l + r },
                Operation("-=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java])
                { l, r -> l - r },
                Operation("*=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java])
                { l, r -> l * r },
                Operation("/=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java])
                { l, r -> l / r },
                Operation("%=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java])
                { l, r -> l % r },
                Operation("&=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java])
                { l, r -> l and r },
                Operation("|=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java])
                { l, r -> l or r },
                Operation("^=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java])
                { l, r -> l xor r },
                Operation("<<=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java])
                { l, r -> l shl r },
                Operation(">>=", this, this) to DefaultHandlers.Assign(this, Instruction.STORE[float_t::class.java])
                { l, r -> l shr r }
        )
    }
}

object int_t : number_t() {
    override val simpleName = "int_t"
    override fun handle(op: Operation): Operation.Handler<Q1VM.State, List<IR>>? {
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
    override fun handle(op: Operation): Operation.Handler<Q1VM.State, List<IR>>? {
        super.handle(op)?.let { return it }
        if (op.right == int_t || op.right == bool_t) {
            return super.handle(op.copy(right = float_t))
        }
        return null
    }
}
