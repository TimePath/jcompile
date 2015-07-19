package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.types.array_t
import com.timepath.compiler.backend.q1vm.types.entity_t
import com.timepath.compiler.types.Operation.Handler
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.struct_t
import com.timepath.compiler.backend.q1vm.Instruction
import com.timepath.compiler.backend.q1vm.types.float_t
import com.timepath.with

object DefaultHandlers {

    fun Binary(type: Type, instr: Instruction) = Handler.Binary<Q1VM.State, List<IR>>(type) { l, r ->
        linkedListOf<IR>() with {
            val genLeft = l.generate()
            addAll(genLeft)
            val genRight = r.generate()
            addAll(genRight)
            val out = allocator.allocateReference(type = type)
            add(IR(instr, arrayOf(genLeft.last().ret, genRight.last().ret, out.ref), out.ref, name = "$l $instr $r"))
        }
    }

    fun Unary(type: Type, instr: Instruction) = Handler.Unary<Q1VM.State, List<IR>>(type) {
        linkedListOf<IR>() with {
            val genLeft = it.generate()
            addAll(genLeft)
            val out = allocator.allocateReference(type = type)
            add(IR(instr, arrayOf(genLeft.last().ret, out.ref), out.ref, name = "$it"))
        }
    }

    /**
     * TODO: other storeps
     */
    fun Assign(type: Type,
               instr: Instruction,
               op: (left: Expression, right: Expression) -> BinaryExpression? = { left, right -> null })
            = Handler.Binary<Q1VM.State, List<IR>>(type) { l, r ->
        fun MutableList<IR>.x(realInstr: Instruction,
                              leftR: Expression,
                              leftL: Expression) {
            val genL = leftL.generate()
            addAll(genL)
            val right = op(leftR, r) ?: r
            val genR = right.generate()
            addAll(genR)

            val lvalue = genL.last()
            val rvalue = genR.last()
            add(IR(realInstr, arrayOf(rvalue.ret, lvalue.ret), rvalue.ret, "$leftL = $right"))
        }
        linkedListOf<IR>() with {
            when (l) {
                is IndexExpression -> {
                    val typeL = l.left.type(this@Binary)
                    when (typeL) {
                        is entity_t -> {
                            val tmp = MemoryReference(l.left.generate().with { addAll(this) }.last().ret, typeL)
                            x(Instruction.STOREP(javaClass<float_t>()),
                                    IndexExpression(tmp, l.right),
                                    IndexExpression(tmp, l.right) with {
                                        this.instr = Instruction.ADDRESS
                                    })
                        }
                        is array_t -> {
                            val arr = l.left
                            val idx = l.right
                            val set = r
                            if (arr !is ReferenceExpression) throw UnsupportedOperationException()
                            val s = typeL.generateAccessorName(arr.refers.id)
                            val resolve = symbols.get(s) ?: throw RuntimeException("Can't resolve $s")
                            val indexer = resolve.ref()
                            val accessor = MethodCallExpression(indexer, listOf(idx))
                            addAll(MethodCallExpression(accessor, listOf(1.expr(), set)).generate())
                        }
                        else -> throw UnsupportedOperationException("Indexing ${typeL}")
                    }
                }
                is MemberExpression -> {
                    val typeL = l.left.type(this@Binary)
                    when (typeL) {
                        is entity_t -> {
                            val tmp = MemoryReference(l.left.generate().with { addAll(this) }.last().ret, typeL)
                            x(Instruction.STOREP(javaClass<float_t>()),
                                    MemberExpression(tmp, l.field),
                                    MemberExpression(tmp, l.field) with {
                                        this.instr = Instruction.ADDRESS
                                    })
                        }
                        is struct_t -> {
                            x(instr, l, l)
                        }
                        else -> throw UnsupportedOperationException("MemberExpression for type ${typeL}")
                    }
                }
                else -> x(instr, l, l)
            }
        }
    }
}
