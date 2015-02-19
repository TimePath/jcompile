package com.timepath.compiler

import com.timepath.compiler.Type.Operation
import com.timepath.compiler.ast.BinaryExpression
import com.timepath.compiler.ast.ConditionalExpression
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.ast.FunctionExpression
import com.timepath.compiler.ast.IndexExpression
import com.timepath.compiler.ast.MemoryReference
import com.timepath.compiler.ast.MethodCallExpression
import com.timepath.compiler.ast.MemberExpression
import com.timepath.compiler.ast.ParameterExpression
import com.timepath.compiler.ast.ReferenceExpression
import com.timepath.compiler.ast.ReturnStatement
import com.timepath.compiler.ast.StructDeclarationExpression
import com.timepath.compiler.ast.UnaryExpression
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.q1vm.Instruction
import kotlin.properties.Delegates

abstract class Type {

    class object {
        fun from(any: Any?): Type = when (any) {
            is kotlin.Float -> Float
            is kotlin.Int -> Int
            is kotlin.Boolean -> Bool
            is kotlin.String -> String
            is com.timepath.compiler.Vector -> Vector
            else -> throw NullPointerException()
        }

        fun handle(operation: Operation): Type.OperationHandler {
            operation.left.handle(operation)?.let {
                return it
            }
            Void.handle(operation.copy(left = Void, right = when {
                operation.right != null -> Void
                else -> null
            }))?.let {
                return it
            }
            throw UnsupportedOperationException("$operation")
        }
    }

    open fun declare(name: kotlin.String, value: ConstantExpression? = null): List<Expression> {
        throw UnsupportedOperationException()
    }

    override fun toString(): kotlin.String {
        return javaClass.getSimpleName().toLowerCase()
    }

    abstract val ops: Map<Operation, OperationHandler>

    open fun handle(op: Operation): OperationHandler? = ops[op]

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
            add(IR(instr, array(genLeft.last().ret, genRight.last().ret, out.ref), out.ref, name = "$left $instr $right"))
            this
        }
    })

    class DefaultUnaryHandler(type: Type, instr: Instruction) : OperationHandler(type, { gen, self, _ ->
        with(linkedListOf<IR>()) {
            val genLeft = self.doGenerate(gen)
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
                    val typeL = left.left.type(gen)
                    val tmp = left.left.doGenerate(gen)
                    addAll(tmp)
                    val refE = tmp.last().ret

                    realInstr = Instruction.STOREP_FLOAT
                    val memoryReference = MemoryReference(refE, typeL)
                    leftR = IndexExpression(memoryReference, left.right)
                    leftL = IndexExpression(memoryReference, left.right).let {
                        it.instr = Instruction.ADDRESS
                        it
                    }
                }
                left is MemberExpression -> {
                    val typeL = left.left.type(gen)
                    // get the entity
                    val tmp = left.left.doGenerate(gen)
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

    object Int : Number() {
        override fun handle(op: Operation): OperationHandler? {
            super.handle(op)?.let {
                return it
            }
            if (op.right == Float) {
                return Float.handle(op.copy(left = Float))
            }
            return null
        }
    }

    object Float : Number() {
        override fun handle(op: Operation): OperationHandler? {
            super.handle(op)?.let {
                return it
            }
            if (op.right == Int) {
                return super.handle(op.copy(right = Float))
            }
            return null
        }
    }

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

                    Operation("*", this, this) to DefaultHandler(this, Instruction.MUL_FLOAT),
                    Operation("*", this, Vector) to DefaultHandler(Vector, Instruction.MUL_FLOAT_VEC),
                    Operation("/", this, this) to DefaultHandler(this, Instruction.DIV_FLOAT),
                    Operation("%", this, this) to OperationHandler(this) { gen, left, right ->
                        MethodCallExpression(ReferenceExpression("__builtin_mod"), listOf(left, right!!)).doGenerate(gen)
                    },

                    // pre
                    Operation("++", this) to OperationHandler(this) { gen, self, _ ->
                        val one = UnaryExpression.Cast(Type.Int, ConstantExpression(1f))
                        BinaryExpression.Assign(self, BinaryExpression.Add(self, one)).doGenerate(gen)
                    },
                    // post
                    Operation("++", this, this) to OperationHandler(this) { gen, self, _ ->
                        val one = UnaryExpression.Cast(Type.Int, ConstantExpression(1f))
                        with(linkedListOf<IR>()) {
                            val add = BinaryExpression.Add(self, one)
                            val assign = BinaryExpression.Assign(self, add)
                            // FIXME
                            val sub = BinaryExpression.Subtract(assign, one)
                            addAll(sub.doGenerate(gen))
                            this
                        }
                    },
                    // pre
                    Operation("--", this) to OperationHandler(this) { gen, self, _ ->
                        val one = UnaryExpression.Cast(Type.Int, ConstantExpression(1f))
                        BinaryExpression.Assign(self, BinaryExpression.Subtract(self, one)).doGenerate(gen)
                    },
                    // post
                    Operation("--", this, this) to OperationHandler(this) { gen, self, _ ->
                        val one = UnaryExpression.Cast(Type.Int, ConstantExpression(1f))
                        with(linkedListOf<IR>()) {
                            val sub = BinaryExpression.Subtract(self, one)
                            val assign = BinaryExpression.Assign(self, sub)
                            // FIXME
                            val add = BinaryExpression.Add(assign, one)
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
                Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_VEC),
                Operation("==", this, this) to DefaultHandler(Bool, Instruction.EQ_VEC),
                Operation("!=", this, this) to DefaultHandler(Bool, Instruction.NE_VEC),
                Operation("!", this) to DefaultUnaryHandler(Bool, Instruction.NOT_VEC),
                Operation("*", this, Float) to DefaultHandler(this, Instruction.MUL_VEC_FLOAT),
                Operation("*", this, Int) to DefaultHandler(this, Instruction.MUL_VEC_FLOAT),
                Operation("*=", this, Float) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                    BinaryExpression.Multiply(left, right)
                },
                Operation("+", this, this) to DefaultHandler(this, Instruction.ADD_VEC),
                Operation("+=", this, this) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                    BinaryExpression.Add(left, right)
                },
                Operation("-", this, this) to DefaultHandler(this, Instruction.SUB_VEC),
                Operation("-=", this, this) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                    BinaryExpression.Subtract(left, right)
                },
                Operation("|", this, this) to OperationHandler(this) { gen, left, right ->
                    with(linkedListOf<IR>()) {
                        val ref = gen.allocator.allocateReference(type = this@Vector)
                        val genL = left.doGenerate(gen)
                        addAll(genL)
                        val genR = right!!.doGenerate(gen)
                        addAll(genR)
                        val lhs = genL.last()
                        val rhs = genR.last()
                        for (i in 0..2) {
                            val component = MemoryReference(ref.ref + i, type = Float)
                            addAll(BinaryExpression.Assign(component, BinaryExpression.BitOr(
                                    MemoryReference(lhs.ret + i, type = Float),
                                    MemoryReference(rhs.ret + i, type = Float)
                            )).doGenerate(gen))
                        }
                        this
                    }
                },
                Operation("|=", this, this) to DefaultAssignHandler(this, Instruction.STORE_VEC) { left, right ->
                    BinaryExpression.BitOr(left, right)
                }
        )
    }

    abstract class Pointer : Type()

    object String : Pointer() {
        override val ops = mapOf(
                Operation("=", this, this) to DefaultAssignHandler(this, Instruction.STORE_STR),
                Operation("==", this, this) to DefaultHandler(Bool, Instruction.EQ_STR),
                Operation("!=", this, this) to DefaultHandler(Bool, Instruction.NE_STR),
                Operation("!", this) to DefaultUnaryHandler(Bool, Instruction.NOT_STR)
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
                },
                Operation("==", this, this) to DefaultHandler(Bool, Instruction.EQ_ENT),
                Operation("!=", this, this) to DefaultHandler(Bool, Instruction.NE_ENT),
                Operation("!", this) to DefaultUnaryHandler(Bool, Instruction.NOT_ENT),
                Operation("(int)", this) to OperationHandler(Int) { gen, self, _ ->
                    MemoryReference(self.generate(gen).last().ret, Int).doGenerate(gen)
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
                Operation("==", this, this) to DefaultHandler(Bool, Instruction.EQ_FUNC),
                Operation("!=", this, this) to DefaultHandler(Bool, Instruction.NE_FUNC),
                Operation("!", this) to DefaultUnaryHandler(Bool, Instruction.NOT_FUNC),
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
                },
                Operation("[]", this, Int) to OperationHandler(type) { gen, left, right ->
                    val s = generateAccessorName((left as ReferenceExpression).id)
                    val indexer = MethodCallExpression(ReferenceExpression(s), listOf(right!!))
                    MethodCallExpression(indexer, listOf(ConstantExpression(0))).doGenerate(gen)
                }
        )

        override fun declare(name: kotlin.String, value: ConstantExpression?): List<Expression> {
            val size = (sizeExpr.evaluate()?.value as kotlin.Number).toInt()
            val intRange = size.indices
            return with(linkedListOf<Expression>()) {
                add(DeclarationExpression(name, this@Array))
                add(DeclarationExpression("${name}_size", Int, ConstantExpression(size.toFloat())))
                add(generateAccessor(name))
                intRange.forEachIndexed {(i, _) ->
                    addAll(generateComponent(name, i))
                }
                this
            }
        }

        private fun generateAccessorName(id: kotlin.String) = "__${id}_access"

        /**
         *  #define ARRAY(name, size)                                                       \
         *      float name##_size = size;                                                   \
         *      float(bool, float) name##_access(float index) {                             \
         *          /*if (index < 0) index += name##_size;                                  \
         *          if (index > name##_size) return 0;*/                                    \
         *          float(bool, float) name##_access_this = *(&name##_access + 1 + index);  \
         *          return name##_access_this;                                              \
         *      }
         */
        private fun generateAccessor(id: kotlin.String): Expression {
            val accessor = generateAccessorName(id)
            return FunctionExpression(
                    accessor,
                    Function(Function(Float, listOf(Float, Float), null), listOf(Float), null),
                    listOf(ParameterExpression("index", Float, 0)),
                    add = listOf(ReturnStatement(
                            UnaryExpression.Dereference(BinaryExpression.Add(
                                    UnaryExpression.Address(
                                            ReferenceExpression(accessor)
                                    ),
                                    BinaryExpression.Add(
                                            ConstantExpression(1f),
                                            ReferenceExpression("index")
                                    )
                            ))
                    ))
            )
        }

        /**
         *  #define ARRAY_COMPONENT(name, i, v)                         \
         *      float name##_##i = v;                                   \
         *      float name##_access_##i(bool mode, float value##i) {    \
         *          return mode ? name##_##i = value##i : name##_##i;   \
         *      }
         */
        private fun generateComponent(id: kotlin.String, i: kotlin.Int): List<Expression> {
            val accessor = "${generateAccessorName(id)}_${i}"
            val field = "${accessor}_field"
            return with(linkedListOf<Expression>()) {
                add(DeclarationExpression(field, type))
                val fieldReference = ReferenceExpression(field)
                add(FunctionExpression(
                        accessor,
                        Function(Float, listOf(Float, Float), null),
                        listOf(
                                ParameterExpression("mode", Float, 0),
                                ParameterExpression("value", Float, 1)
                        ),
                        add = listOf(ReturnStatement(ConditionalExpression(
                                ReferenceExpression("mode"), true,
                                BinaryExpression.Assign(fieldReference, ReferenceExpression("value")),
                                fieldReference
                        )))
                ))
                this
            }
        }

        override fun toString(): kotlin.String {
            return "${super.toString()}($type[$sizeExpr])"
        }
    }
}
