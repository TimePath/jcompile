package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.ast.*
import com.timepath.compiler.types.OperationHandler
import com.timepath.compiler.types.Type
import com.timepath.q1vm.Instruction

object DefaultHandlers {

    fun Binary(type: Type, instr: Instruction) = OperationHandler.Binary<Q1VM.State, List<IR>>(type, { left, right ->
        with(linkedListOf<IR>()) {
            val genLeft = left.generate()
            addAll(genLeft)
            val genRight = right.generate()
            addAll(genRight)
            val out = allocator.allocateReference(type = type)
            add(IR(instr, array(genLeft.last().ret, genRight.last().ret, out.ref), out.ref, name = "$left $instr $right"))
            this
        }
    })

    fun Unary(type: Type, instr: Instruction) = OperationHandler.Unary<Q1VM.State, List<IR>>(type, {
        with(linkedListOf<IR>()) {
            val genLeft = it.generate()
            addAll(genLeft)
            val out = allocator.allocateReference(type = type)
            add(IR(instr, array(genLeft.last().ret, out.ref), out.ref, name = "$it"))
            this
        }
    })

    fun Assign(type: Type,
               instr: Instruction,
               op: (left: Expression, right: Expression) -> BinaryExpression? = { left, right -> null })
            = OperationHandler.Binary<Q1VM.State, List<IR>>(type, { lhs, rhs ->
        with(linkedListOf<IR>()) {
            val realInstr: Instruction
            val leftL: Expression
            val leftR: Expression
            // TODO: other storeps
            when {
                lhs is IndexExpression -> {
                    // TODO: returning arrays
                    // val typeL = left.left.type(gen)
                    // val tmp = left.left.doGenerate(gen)
                    // addAll(tmp)
                    // val refE = tmp.last().ret
                    // val memoryReference = MemoryReference(refE, typeL)
                    val memoryReference = lhs.left
                    realInstr = Instruction.STOREP_FLOAT
                    leftR = IndexExpression(memoryReference, lhs.right)
                    leftL = IndexExpression(memoryReference, lhs.right).let {
                        it.instr = Instruction.ADDRESS
                        it
                    }
                }
                lhs is MemberExpression -> {
                    val typeL = lhs.left.type(this@Binary)
                    // get the entity
                    val tmp = lhs.left.generate()
                    addAll(tmp)
                    val refE = tmp.last().ret

                    realInstr = Instruction.STOREP_FLOAT
                    val memoryReference = MemoryReference(refE, typeL)
                    leftR = MemberExpression(memoryReference, lhs.field)
                    leftL = MemberExpression(memoryReference, lhs.field).let {
                        it.instr = Instruction.ADDRESS
                        it
                    }
                }
                else -> {
                    realInstr = instr
                    leftR = lhs
                    leftL = lhs
                }
            }
            val genL = leftL.generate()
            addAll(genL)
            val genR = (op(leftR, rhs) ?: rhs).generate()
            addAll(genR)

            val lvalue = genL.last()
            val rvalue = genR.last()
            add(IR(realInstr, array(rvalue.ret, lvalue.ret), rvalue.ret, "$leftL = $genR"))
            this
        }
    })
}
