package com.timepath.quakec.compiler

import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.compiler.ast.BinaryExpression
import com.timepath.quakec.compiler.ast.ConditionalExpression
import com.timepath.quakec.compiler.ast.ConstantExpression
import com.timepath.quakec.compiler.ast.DeclarationExpression
import com.timepath.quakec.compiler.ast.Expression
import com.timepath.quakec.compiler.ast.MemberExpression
import com.timepath.quakec.compiler.ast.MethodCallExpression
import com.timepath.quakec.compiler.ast.ReferenceExpression
import com.timepath.quakec.compiler.ast.StructDeclarationExpression
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR

abstract class Type {

    class object {
        fun from(any: Any?): Type = when (any) {
            is kotlin.Float -> Float
            is kotlin.Int -> Int()
            is kotlin.String -> String
            else -> Void
        }

        fun handle(operation: Operation): Type.OperationHandler {
            val primary = Float.ops[operation]
            if (primary != null) {
                return primary
            }
            return Void.ops[operation.copy(left = Void, right = when {
                operation.right != null -> Void
                else -> null
            })]!!
        }
    }

    open fun declare(name: kotlin.String, value: ConstantExpression? = null): List<DeclarationExpression> {
        throw UnsupportedOperationException()
    }

    override fun toString(): kotlin.String {
        return javaClass.getSimpleName()
    }

    abstract val ops: Map<Operation, OperationHandler>

    data class Operation(val op: kotlin.String, val left: Type, val right: Type? = null)

    open class OperationHandler(protected val handle: (gen: Generator, left: Expression, right: Expression?) -> List<IR>) {
        fun invoke(gen: Generator, left: Expression, right: Expression? = null): List<IR> = handle(gen, left, right)
    }

    class DefaultHandler(instr: Instruction) : OperationHandler({ gen, left, right ->
        right!!
        with(linkedListOf<IR>()) {
            val genLeft = left.doGenerate(gen)
            addAll(genLeft)
            val genRight = right.doGenerate(gen)
            addAll(genRight)
            val out = gen.allocator.allocateReference()
            add(IR(instr, array(genLeft.last().ret, genRight.last().ret, out.ref), out.ref))
            this
        }
    })

    class DefaultAssignHandler(instr: Instruction) : OperationHandler({ gen, left, right ->
        with(linkedListOf<IR>()) {
            val lvalue = when (left) {
                is MemberExpression -> {
                    // make a copy to avoid changing the right half of the assignment
                    val special = MemberExpression(left.left, left.right)
                    special.instr = Instruction.ADDRESS
                    special
                }
                else -> left
            }
            val realInstr = when {
                lvalue is MemberExpression -> Instruction.STOREP_FLOAT // TODO
                else -> instr
            }
            val genLeft = lvalue.doGenerate(gen)
            addAll(genLeft)
            val genRight = right!!.doGenerate(gen)
            addAll(genRight)
            val out = genLeft.last()
            add(IR(realInstr, array(genRight.last().ret, out.ret), out.ret))
            this
        }
    })

    object Void : Type() {
        override val ops = mapOf(
                Operation(",", this, this) to OperationHandler { gen, left, right ->
                    with(linkedListOf<IR>()) {
                        addAll(left.doGenerate(gen))
                        addAll(right!!.doGenerate(gen))
                        this
                    }
                },
                Operation("&&", this, this) to OperationHandler({ gen, left, right ->
                    // TODO: Instruction.AND when no side effects
                    ConditionalExpression(left,
                            fail = ConstantExpression(0),
                            pass = ConditionalExpression(right!!,
                                    fail = ConstantExpression(0),
                                    pass = ConstantExpression(1f))
                    ).doGenerate(gen)
                }),
                Operation("||", this, this) to OperationHandler({ gen, left, right ->
                    // TODO: Instruction.OR when no side effects
                    ConditionalExpression(left,
                            pass = ConstantExpression(1f),
                            fail = ConditionalExpression(right!!,
                                    pass = ConstantExpression(1f),
                                    fail = ConstantExpression(0))
                    ).doGenerate(gen)
                })
        )
    }

    object Float : Type() {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(Instruction.STORE_FLOAT),
                Operation("+", this, this) to DefaultHandler(Instruction.ADD_FLOAT),
                Operation("-", this, this) to DefaultHandler(Instruction.SUB_FLOAT),
                Operation("+", this) to OperationHandler { gen, self, _ ->
                    self.doGenerate(gen)
                },
                Operation("-", this) to OperationHandler { gen, self, _ ->
                    BinaryExpression.Sub(ConstantExpression(0f), self).doGenerate(gen)
                },
                Operation("*", this, this) to DefaultHandler(Instruction.MUL_FLOAT),
                Operation("/", this, this) to DefaultHandler(Instruction.DIV_FLOAT),
                Operation("%", this, this) to OperationHandler { gen, left, right ->
                    MethodCallExpression(ReferenceExpression("__builtin_mod"), listOf(left, right!!)).doGenerate(gen)
                },
                // pre
                Operation("++", this) to OperationHandler { gen, self, _ ->
                    BinaryExpression.Assign(self, BinaryExpression.Add(self, ConstantExpression(1f))).doGenerate(gen)
                },
                // post
                Operation("++", this, this) to OperationHandler { gen, self, _ ->
                    with(linkedListOf<IR>()) {
                        val add = BinaryExpression.Add(self, ConstantExpression(1f))
                        val assign = BinaryExpression.Assign(self, add)
                        // FIXME
                        val sub = BinaryExpression.Sub(assign, ConstantExpression(1f))
                        addAll(sub.doGenerate(gen))
                        this
                    }
                },
                // pre
                Operation("--", this) to OperationHandler { gen, self, _ ->
                    BinaryExpression.Assign(self, BinaryExpression.Sub(self, ConstantExpression(1f))).doGenerate(gen)
                },
                // post
                Operation("--", this, this) to OperationHandler { gen, self, _ ->
                    with(linkedListOf<IR>()) {
                        val sub = BinaryExpression.Sub(self, ConstantExpression(1f))
                        val assign = BinaryExpression.Assign(self, sub)
                        // FIXME
                        val add = BinaryExpression.Add(assign, ConstantExpression(1f))
                        addAll(add.doGenerate(gen))
                        this
                    }
                },
                Operation("==", this, this) to DefaultHandler(Instruction.EQ_FLOAT),
                Operation("!=", this, this) to DefaultHandler(Instruction.NE_FLOAT),
                Operation(">", this, this) to DefaultHandler(Instruction.GT),
                Operation("<", this, this) to DefaultHandler(Instruction.LT),
                Operation(">=", this, this) to DefaultHandler(Instruction.GE),
                Operation("<=", this, this) to DefaultHandler(Instruction.LE),

                Operation("!", this) to OperationHandler { gen, self, _ ->
                    BinaryExpression.Eq(ConstantExpression(0f), self).doGenerate(gen)
                },
                Operation("~", this) to OperationHandler { gen, self, _ ->
                    BinaryExpression.Sub(ConstantExpression(-1f), self).doGenerate(gen)
                },
                Operation("&", this, this) to DefaultHandler(Instruction.BITAND),
                Operation("|", this, this) to DefaultHandler(Instruction.BITOR),
                Operation("^", this, this) to OperationHandler { gen, left, right ->
                    MethodCallExpression(ReferenceExpression("__builtin_xor"), listOf(left, right!!)).doGenerate(gen)
                }
                // TODO
//                Operation("<<", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation(">>", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation("**", this, this) to DefaultHandler(Instruction.BITOR),
                // TODO
//                Operation("+=", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation("-=", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation("*=", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation("/=", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation("%=", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation("*=", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation("|=", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation("^=", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation("<<=", this, this) to DefaultHandler(Instruction.BITOR),
//                Operation(">>=", this, this) to DefaultHandler(Instruction.BITOR)
        )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }

    open class Int : Type() {
        override val ops: Map<Operation, OperationHandler>
            get() = throw UnsupportedOperationException()
    }

    data abstract class Struct(val fields: Map<kotlin.String, Type>) : Type() {
        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(StructDeclarationExpression(name, this))
        }
    }

    object Vector : Struct(mapOf("x" to Float, "y" to Float, "z" to Float)) {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(Instruction.STORE_VEC)
        )
    }

    abstract class Pointer : Int()

    object String : Pointer() {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(Instruction.STORE_STR)
        )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }

    object Entity : Pointer() {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(Instruction.STORE_ENT)
        )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }

    data class Field(val type: Type) : Pointer() {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(Instruction.STORE_FIELD)
        )

        override fun toString(): kotlin.String {
            return "${super.toString()}($type)"
        }
    }

    data class Function(val type: Type, val argTypes: List<Type>) : Pointer() {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(Instruction.STORE_FUNC)
        )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }

        override fun toString(): kotlin.String {
            return "${super.toString()}($type, $argTypes)"
        }
    }
}