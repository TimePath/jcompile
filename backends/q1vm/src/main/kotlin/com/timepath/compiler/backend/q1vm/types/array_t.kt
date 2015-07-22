package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.evaluate
import com.timepath.compiler.ir.IR
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t
import com.timepath.compiler.types.defaults.pointer_t
import com.timepath.with

data class array_t(val type: Type, val sizeExpr: Expression, val state: CompileState) : pointer_t() {

    override val simpleName = "array_t"
    override fun toString() = "$type[$sizeExpr]"

    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("sizeof", this) to Operation.Handler.Unary<Q1VM.State, List<IR>>(int_t) {
                sizeExpr.generate()
            },
            Operation("[]", this, int_t) to Operation.Handler.Binary<Q1VM.State, List<IR>>(type) { l, r ->
                if (l !is ReferenceExpression) throw UnsupportedOperationException()
                // arr[i] -> arr(i)(false)
                val s = generateAccessorName(l.refers.id)
                val resolve = state.symbols[s] ?: throw RuntimeException("Can't resolve $s")
                val accessor = resolve.ref()
                val indexer = MethodCallExpression(accessor, listOf(r))
                MethodCallExpression(indexer, listOf(false.expr())).generate()
            }
    )

    // FIXME
    override fun declare(name: String, value: ConstantExpression?, state: CompileState): List<Expression> {
        val sizeVal = sizeExpr.evaluate(state as Q1VM.State)
        val size = sizeVal?.let { (it.any as Number).toInt() } ?: -1
        return linkedListOf<Expression>() with {
            add(DeclarationExpression(name, this@array_t))
            add(DeclarationExpression("${name}_size", int_t, size.expr()))
            add(generateAccessor(name))
            repeat(size) {
                addAll(generateComponent(name, it))
            }
        }
    }

    fun generateAccessorName(id: String) = "__${id}_access"

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
    private fun generateAccessor(id: String): Expression {
        val accessor = generateAccessorName(id)
        val index = ParameterExpression("index", int_t, 0)
        val func = FunctionExpression(
                accessor,
                function_t(function_t(type, listOf(bool_t, type), null), listOf(int_t), null),
                listOf(index)
        )
        val e = (func.ref().address()
                + 1.expr()
                + index.ref()
                ).deref()
        func.add(ReturnStatement(e))
        return func
    }

    /**
     *  #define ARRAY_COMPONENT(name, i, v)                         \
     *      float name##_##i = v;                                   \
     *      float name##_access_##i(bool mode, float value##i) {    \
     *          return mode ? name##_##i = value##i : name##_##i;   \
     *      }
     */
    private fun generateComponent(id: String, i: Int): List<Expression> {
        val accessor = "${generateAccessorName(id)}_${i}"
        return linkedListOf<Expression>() with {
            val field = DeclarationExpression("${accessor}_field", type)
            add(field)
            val mode = ParameterExpression("mode", bool_t, 0)
            val value = ParameterExpression("value", type, 1)
            add(FunctionExpression(
                    accessor,
                    function_t(type, listOf(bool_t, type), null),
                    listOf(
                            mode,
                            value
                    ),
                    add = listOf(ReturnStatement(ConditionalExpression(
                            mode.ref(), true,
                            field.ref().set(value.ref()),
                            field.ref()
                    )))
            ))
        }
    }

}
