package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.ast.*
import com.timepath.compiler.types.OperationHandler
import com.timepath.compiler.types.Type
import com.timepath.q1vm.Instruction

class DefaultHandler(type: Type, instr: Instruction) : OperationHandler<Q1VM.State, List<IR>>(type, { gen, left, right ->
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

class DefaultUnaryHandler(type: Type, instr: Instruction) : OperationHandler<Q1VM.State, List<IR>>(type, { gen, self, _ ->
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
: OperationHandler<Q1VM.State, List<IR>>(type, { gen, left, right ->
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
        add(IR(realInstr, array(rvalue.ret, lvalue.ret), rvalue.ret, "$leftL = $right"))
        this
    }
})
