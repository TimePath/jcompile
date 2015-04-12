package com.timepath.compiler.types

import com.timepath.compiler.gen.generate
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.type
import com.timepath.compiler.gen.IR
import com.timepath.compiler.ast.*
import com.timepath.compiler.Vector
import com.timepath.q1vm.Instruction
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.Named
import com.timepath.compiler.Pointer

trait Type: Named {

    companion object {
        fun from(any: Any?): Type = when (any) {
            is Float -> float_t
            is Int -> int_t
            is Boolean -> bool_t
            is String -> string_t
            is Vector -> vector_t
            is Char -> int_t
            is Pointer -> int_t
            else -> throw NoWhenBranchMatchedException()
        }

        fun type(operation: Operation) = handle(operation).type

        fun handle(operation: Operation): OperationHandler {
            if (operation.op == ",") {
                return OperationHandler(operation.right!!) { gen, left, right ->
                    with(linkedListOf<IR>()) {
                        addAll(left.generate(gen))
                        addAll(right!!.generate(gen))
                        this
                    }
                }
            }
            operation.left.handle(operation)?.let {
                return it
            }
            void_t.handle(operation.copy(left = void_t, right = void_t))?.let {
                return it
            }
            throw UnsupportedOperationException("$operation")
        }
    }

    override fun toString() = javaClass.getSimpleName().toLowerCase()

    fun declare(name: String, value: ConstantExpression? = null, state: CompileState? = null): List<Expression>

    fun handle(op: Operation): OperationHandler?

}

data class Operation(val op: String, val left: Type, val right: Type? = null)

open class OperationHandler(val type: Type, protected val handle: (state: CompileState, left: Expression, right: Expression?) -> List<IR>) {

    fun invoke(state: CompileState, left: Expression, right: Expression? = null): List<IR> = handle(state, left, right)
}

class DefaultHandler(type: Type, instr: Instruction) : OperationHandler(type, { gen, left, right ->
    right!!
    with(linkedListOf<IR>()) {
        val genLeft = left.generate(gen)
        addAll(genLeft)
        val genRight = right.generate(gen)
        addAll(genRight)
        val out = gen.allocator.allocateReference(type = type)
        add(IR(instr, array(genLeft.last().ret, genRight.last().ret, out.ref), out.ref, name = "$left $instr $right"))
        this
    }
})

class DefaultUnaryHandler(type: Type, instr: Instruction) : OperationHandler(type, { gen, self, _ ->
    with(linkedListOf<IR>()) {
        val genLeft = self.generate(gen)
        addAll(genLeft)
        val out = gen.allocator.allocateReference(type = type)
        add(IR(instr, array(genLeft.last().ret, out.ref), out.ref, name = "$self"))
        this
    }
})

class DefaultAssignHandler(type: Type,
                           instr: Instruction,
                           op: (left: Expression, right: Expression) -> BinaryExpression? = { left, right -> null })
: OperationHandler(type, { gen, left, right ->
    with(linkedListOf<IR>()) {
        val realInstr: Instruction
        val leftL: Expression
        val leftR: Expression
        // TODO: other storeps
        when {
            left is IndexExpression -> {
                // TODO: returning arrays
                // val typeL = left.left.type(gen)
                // val tmp = left.left.doGenerate(gen)
                // addAll(tmp)
                // val refE = tmp.last().ret
                // val memoryReference = MemoryReference(refE, typeL)
                val memoryReference = left.left
                realInstr = Instruction.STOREP_FLOAT
                leftR = IndexExpression(memoryReference, left.right)
                leftL = IndexExpression(memoryReference, left.right).let {
                    it.instr = Instruction.ADDRESS
                    it
                }
            }
            left is MemberExpression -> {
                val typeL = left.left.type(gen)
                // get the entity
                val tmp = left.left.generate(gen)
                addAll(tmp)
                val refE = tmp.last().ret

                realInstr = Instruction.STOREP_FLOAT
                val memoryReference = MemoryReference(refE, typeL)
                leftR = MemberExpression(memoryReference, left.field)
                leftL = MemberExpression(memoryReference, left.field).let {
                    it.instr = Instruction.ADDRESS
                    it
                }
            }
            else -> {
                realInstr = instr
                leftR = left
                leftL = left
            }
        }
        val lhs = leftL.generate(gen)
        addAll(lhs)
        val rhs = (op(leftR, right!!) ?: right).generate(gen)
        addAll(rhs)

        val lvalue = lhs.last()
        val rvalue = rhs.last()
        add(IR(realInstr, array(rvalue.ret, lvalue.ret), rvalue.ret))
        this
    }
})
