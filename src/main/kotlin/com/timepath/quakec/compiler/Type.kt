package com.timepath.quakec.compiler

import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.compiler.ast.BinaryExpression
import com.timepath.quakec.compiler.ast.ConditionalExpression
import com.timepath.quakec.compiler.ast.ConstantExpression
import com.timepath.quakec.compiler.ast.DeclarationExpression
import com.timepath.quakec.compiler.ast.Expression
import com.timepath.quakec.compiler.ast.FunctionExpression
import com.timepath.quakec.compiler.ast.IndexExpression
import com.timepath.quakec.compiler.ast.MemoryReference
import com.timepath.quakec.compiler.ast.MethodCallExpression
import com.timepath.quakec.compiler.ast.MemberExpression
import com.timepath.quakec.compiler.ast.ParameterExpression
import com.timepath.quakec.compiler.ast.ReferenceExpression
import com.timepath.quakec.compiler.ast.ReturnStatement
import com.timepath.quakec.compiler.ast.StructDeclarationExpression
import com.timepath.quakec.compiler.ast.UnaryExpression
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import kotlin.properties.Delegates

abstract class Type {

    class object {
        fun from(any: Any?): Type = when (any) {
            is kotlin.Float -> Float
            is kotlin.Int -> Int
            is kotlin.Boolean -> Bool
            is kotlin.String -> String
            else -> throw NullPointerException()
        }

        fun handle(operation: Operation): Type.OperationHandler {
            val ops = operation.left.ops
            val primary = ops[operation]
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
        return javaClass.getSimpleName().toLowerCase()
    }

    abstract val ops: Map<Operation, OperationHandler>

    data class Operation(val op: kotlin.String, val left: Type, val right: Type? = null)

    open class OperationHandler(val type: Type, protected val handle: (gen: Generator, left: Expression, right: Expression?) -> List<IR>) {
        fun invoke(gen: Generator, left: Expression, right: Expression? = null): List<IR> = handle(gen, left, right)
    }

    class DefaultHandler(type: Type, instr: Instruction) : OperationHandler(type, { gen, left, right ->
        right!!
        with(linkedListOf<IR>()) {
            val genLeft = left.doGenerate(gen)
            addAll(genLeft)
            val genRight = right.doGenerate(gen)
            addAll(genRight)
            val out = gen.allocator.allocateReference(type = type)
            add(IR(instr, array(genLeft.last().ret, genRight.last().ret, out.ref), out.ref))
            this
        }
    })

    class DefaultAssignHandler(type: Type,
                               instr: Instruction,
                               op: (left: Expression, right: Expression) -> BinaryExpression<Expression, Expression>? = { left, right -> null })
    : OperationHandler(type, { gen, left, right ->
        with(linkedListOf<IR>()) {
            val realInstr: Instruction
            val leftL: Expression
            val leftR: Expression
            // TODO: other storeps
            when {
                left is IndexExpression -> {
                    val tmp = left.left.doGenerate(gen)
                    addAll(tmp)
                    val refE = tmp.last().ret

                    realInstr = Instruction.STOREP_FLOAT
                    val memoryReference = MemoryReference(refE, Type.Entity)
                    leftR = IndexExpression(memoryReference, left.right)
                    leftL = IndexExpression(memoryReference, left.right).let {
                        it.instr = Instruction.ADDRESS
                        it
                    }
                }
                left is MemberExpression -> {
                    // get the entity
                    val tmp = left.left.doGenerate(gen)
                    addAll(tmp)
                    val refE = tmp.last().ret

                    realInstr = Instruction.STOREP_FLOAT
                    val memoryReference = MemoryReference(refE, Type.Entity)
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
            val lhs = leftL.doGenerate(gen)
            addAll(lhs)
            val rhs = (op(leftR, right!!) ?: right).doGenerate(gen)
            addAll(rhs)

            val lvalue = lhs.last()
            val rvalue = rhs.last()
            add(IR(realInstr, array(rvalue.ret, lvalue.ret), rvalue.ret))
            this
        }
    })

    object Void : Type() {
        override val ops = mapOf(
                Operation(",", this, this) to OperationHandler(this) { gen, left, right ->
                    with(linkedListOf<IR>()) {
                        addAll(left.doGenerate(gen))
                        addAll(right!!.doGenerate(gen))
                        this
                    }
                },
                Operation("&&", this, this) to OperationHandler(Bool) { gen, left, right ->
                    // TODO: Instruction.AND when no side effects
                    ConditionalExpression(left, true,
                            fail = ConstantExpression(0),
                            pass = ConditionalExpression(right!!, true,
                                    fail = ConstantExpression(0),
                                    pass = ConstantExpression(1f))
                    ).doGenerate(gen)
                },
                // TODO: perl behaviour
                Operation("||", this, this) to OperationHandler(Bool) { gen, left, right ->
                    // TODO: Instruction.OR when no side effects
                    ConditionalExpression(left, true,
                            pass = ConstantExpression(1f),
                            fail = ConditionalExpression(right!!, true,
                                    pass = ConstantExpression(1f),
                                    fail = ConstantExpression(0))
                    ).doGenerate(gen)
                },
                Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT)
        )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }

    object Bool : Type() {
        override val ops: Map<Operation, OperationHandler>
            get() = mapOf(
                    Operation("==", this, this) to DefaultHandler(Bool, Instruction.EQ_FLOAT),
                    Operation("!=", this, this) to DefaultHandler(Bool, Instruction.NE_FLOAT),
                    Operation("!", this) to OperationHandler(Bool) { gen, self, _ ->
                        BinaryExpression.Eq(ConstantExpression(0f), self).doGenerate(gen)
                    }
            )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }

    object Int : Number()

    object Float : Number()

    open class Number : Type() {
        override val ops by Delegates.lazy {
            mapOf(
                    Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT),
                    Operation("+", this, this) to DefaultHandler(this, Instruction.ADD_FLOAT),
                    Operation("-", this, this) to DefaultHandler(this, Instruction.SUB_FLOAT),
                    Operation("&", this) to OperationHandler(this) { gen, self, _ ->
                        BinaryExpression.Divide(self, ConstantExpression(1)).doGenerate(gen)
                    },
                    Operation("*", this) to OperationHandler(this) { gen, self, _ ->
                        BinaryExpression.Multiply(self, ConstantExpression(1)).doGenerate(gen)
                    },
                    Operation("+", this) to OperationHandler(this) { gen, self, _ ->
                        self.doGenerate(gen)
                    },
                    Operation("-", this) to OperationHandler(this) { gen, self, _ ->
                        BinaryExpression.Subtract(ConstantExpression(0f), self).doGenerate(gen)
                    },
                    Operation("*", this, Float) to DefaultHandler(this, Instruction.MUL_FLOAT),
                    Operation("*", this, Int) to DefaultHandler(this, Instruction.MUL_FLOAT),
                    Operation("/", this, Float) to DefaultHandler(this, Instruction.DIV_FLOAT),
                    Operation("/", this, Int) to DefaultHandler(this, Instruction.DIV_FLOAT),
                    Operation("%", this, this) to OperationHandler(this) { gen, left, right ->
                        MethodCallExpression(ReferenceExpression("__builtin_mod"), listOf(left, right!!)).doGenerate(gen)
                    },
                    // pre
                    Operation("++", this) to OperationHandler(this) { gen, self, _ ->
                        BinaryExpression.Assign(self, BinaryExpression.Add(self, ConstantExpression(1f))).doGenerate(gen)
                    },
                    // post
                    Operation("++", this, this) to OperationHandler(this) { gen, self, _ ->
                        with(linkedListOf<IR>()) {
                            val add = BinaryExpression.Add(self, ConstantExpression(1f))
                            val assign = BinaryExpression.Assign(self, add)
                            // FIXME
                            val sub = BinaryExpression.Subtract(assign, ConstantExpression(1f))
                            addAll(sub.doGenerate(gen))
                            this
                        }
                    },
                    // pre
                    Operation("--", this) to OperationHandler(this) { gen, self, _ ->
                        BinaryExpression.Assign(self, BinaryExpression.Subtract(self, ConstantExpression(1f))).doGenerate(gen)
                    },
                    // post
                    Operation("--", this, this) to OperationHandler(this) { gen, self, _ ->
                        with(linkedListOf<IR>()) {
                            val sub = BinaryExpression.Subtract(self, ConstantExpression(1f))
                            val assign = BinaryExpression.Assign(self, sub)
                            // FIXME
                            val add = BinaryExpression.Add(assign, ConstantExpression(1f))
                            addAll(add.doGenerate(gen))
                            this
                        }
                    },
                    Operation("==", this, this) to DefaultHandler(Bool, Instruction.EQ_FLOAT),
                    Operation("!=", this, this) to DefaultHandler(Bool, Instruction.NE_FLOAT),
                    Operation(">", this, this) to DefaultHandler(Bool, Instruction.GT),
                    Operation("<", this, this) to DefaultHandler(Bool, Instruction.LT),
                    Operation(">=", this, this) to DefaultHandler(Bool, Instruction.GE),
                    Operation("<=", this, this) to DefaultHandler(Bool, Instruction.LE),

                    Operation("!", this) to OperationHandler(Bool) { gen, self, _ ->
                        BinaryExpression.Eq(ConstantExpression(0f), self).doGenerate(gen)
                    },
                    Operation("~", this) to OperationHandler(this) { gen, self, _ ->
                        BinaryExpression.Subtract(ConstantExpression(-1f), self).doGenerate(gen)
                    },
                    Operation("&", this, this) to DefaultHandler(this, Instruction.BITAND),
                    Operation("|", this, this) to DefaultHandler(this, Instruction.BITOR),
                    Operation("^", this, this) to OperationHandler(this) { gen, left, right ->
                        MethodCallExpression(ReferenceExpression("__builtin_xor"), listOf(left, right!!)).doGenerate(gen)
                    },
                    // TODO
                    //                Operation("<<", this, this) to DefaultHandler(Instruction.BITOR),
                    //                Operation(">>", this, this) to DefaultHandler(Instruction.BITOR),
                    //                Operation("**", this, this) to DefaultHandler(Instruction.BITOR),
                    Operation("+=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                        BinaryExpression.Add(left, right)
                    },
                    Operation("-=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                        BinaryExpression.Subtract(left, right)
                    },
                    Operation("*=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                        BinaryExpression.Multiply(left, right)
                    },
                    Operation("/=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                        BinaryExpression.Divide(left, right)
                    },
                    Operation("%=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                        BinaryExpression.Modulo(left, right)
                    },
                    Operation("&=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                        BinaryExpression.And(left, right)
                    },
                    Operation("|=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                        BinaryExpression.Or(left, right)
                    },
                    Operation("^=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                        BinaryExpression.ExclusiveOr(left, right)
                    },
                    Operation("<<=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                        BinaryExpression.Lsh(left, right)
                    },
                    Operation(">>=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FLOAT) { left, right ->
                        BinaryExpression.Rsh(left, right)
                    }
            )
        }

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }

    data abstract class Struct(val fields: Map<kotlin.String, Type>) : Type() {
        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(StructDeclarationExpression(name, this))
        }
    }

    object Vector : Struct(mapOf("x" to Float, "y" to Float, "z" to Float)) {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_VEC)
        )
    }

    abstract class Pointer : Type()

    object String : Pointer() {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_STR)
        )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }

    object Entity : Pointer() {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_ENT),
                Operation(".", this, String) to OperationHandler(this) { gen, left, right ->
                    // TODO: names to fields
                    ConstantExpression(0).doGenerate(gen)
                }
        )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }
    }

    data class Field(val type: Type) : Pointer() {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FIELD)
        )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }

        override fun toString(): kotlin.String {
            return "${super.toString()}($type)"
        }
    }

    data class Function(val type: Type, val argTypes: List<Type>, val vararg: Type? = null) : Pointer() {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_FUNC),
                Operation("&", this) to OperationHandler(Float) { gen, self, _ ->
                    BinaryExpression.Divide(MemoryReference(self.generate(gen).last().ret, Float), ConstantExpression(1)).doGenerate(gen)
                }
        )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }

        override fun toString(): kotlin.String {
            return "${super.toString()}($type, $argTypes${when (vararg) {
                null -> ""
                else -> ", $vararg..."
            }})"
        }
    }

    data class Array(val type: Type, val sizeExpr: Expression) : Pointer() {
        override val ops = mapOf(
                Operation("sizeof", this) to OperationHandler(Int) { gen, self, _ ->
                    sizeExpr.doGenerate(gen)
                }
        )

        fun generate(id: String): List<Expression> {
            val size = (sizeExpr.evaluate()?.value as kotlin.Number).toInt()
            val intRange = size.indices
            return with(linkedListOf<Expression>()) {
                addAll(generateAccessor(id))
                intRange.forEachIndexed {(i, _) ->
                    addAll(generateComponent(id, i))
                }
                this
            }
        }

        private fun generateAccessorName(id: String) = "__${id}_access"

        /**
         *  #define ARRAY(name, size)                                                                   \
         *      float name##_size = size;                                                               \
         *      float(bool, float) name##_access(float index) {                                         \
         *          /*if (index < 0) index += name##_size;                                              \
         *          if (index > name##_size) return 0;*/                                                \
         *          float(bool, float) name##_access_this = name##_access + *(1 + index);               \
         *          return name##_access_this;                                                          \
         *      }
         */
        private fun generateAccessor(id: String): List<Expression> {
            val accessor = generateAccessorName(id)
            return with(linkedListOf<Expression>()) {
                add(DeclarationExpression("${id}_size", Float, ConstantExpression(size.toInt())))
                add(FunctionExpression(
                        accessor,
                        Function(Function(Float, listOf(Float, Float), null), listOf(Float), null),
                        listOf(ParameterExpression("index", Float, 0)),
                        add = listOf(ReturnStatement(BinaryExpression.Add(
                                ReferenceExpression(accessor),
                                UnaryExpression.Dereference(BinaryExpression.Add(
                                        ConstantExpression(1f),
                                        ReferenceExpression("index")
                                ))
                        )))
                ))
                this
            }
        }

        /**
         *  #define ARRAY_COMPONENT(name, i, v)                         \
         *      float name##_##i = v;                                   \
         *      float name##_access_##i(bool mode, float value##i) {    \
         *          return mode ? name##_##i = value##i : name##_##i;   \
         *      }
         */
        private fun generateComponent(id: String, i: kotlin.Int): List<Expression> {
            val accessor = "${generateAccessorName(id)}_${i}"
            val field = "${accessor}_field"
            return with(linkedListOf<Expression>()) {
                add(DeclarationExpression(field, type, ConstantExpression(0f))) // TODO: custom initialization
                FunctionExpression(
                        accessor,
                        Function(Float, listOf(Float, Float), null),
                        listOf(
                                ParameterExpression("mode", Float, 0),
                                ParameterExpression("value", Float, 1)
                        ),
                        add = with(linkedListOf<Expression>()) {
                            val fieldReference = ReferenceExpression(field)
                            add(ReturnStatement(ConditionalExpression(
                                    ReferenceExpression("mode"), true,
                                    BinaryExpression.Assign(fieldReference, ReferenceExpression("value")),
                                    fieldReference
                            )))
                            this
                        }
                )
                this
            }
        }

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<DeclarationExpression> {
            return listOf(DeclarationExpression(name, this, value))
        }

        override fun toString(): kotlin.String {
            return "${super.toString()}($type[$sizeExpr])"
        }
    }
}
