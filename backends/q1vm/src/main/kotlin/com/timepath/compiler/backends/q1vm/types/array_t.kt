package com.timepath.compiler.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.*
import com.timepath.compiler.backends.q1vm.gen.evaluate
import com.timepath.compiler.backends.q1vm.gen.generate
import com.timepath.compiler.types.defaults.function_t
import com.timepath.compiler.types.defaults.pointer_t

data class array_t(val type: Type, val sizeExpr: Expression, val state: CompileState) : pointer_t() {

    override val simpleName = "array_t"
    override fun toString() = "$type[$sizeExpr]"

    val index = OperationHandler(type) { gen, left, right ->
        when (left) {
        // (ent.arr)[i] -> ent.(arr[i])
            is MemberExpression -> {
                val field = ReferenceExpression(state.symbols.resolve(left.field)!!)
                IndexExpression(left.left, IndexExpression(field, right!!)).generate(gen)
            }
        // arr[i] -> arr(i)(false)
            is ReferenceExpression -> {
                val s = generateAccessorName(left.refers.id)
                val resolve = state.symbols.resolve(s)
                if (resolve == null) {
                    throw RuntimeException("Can't resolve $s")
                }
                val accessor = ReferenceExpression(resolve)
                val indexer = MethodCallExpression(accessor, listOf(right!!))
                MethodCallExpression(indexer, listOf(ConstantExpression(false))).generate(gen)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("sizeof", this) to OperationHandler(int_t) { gen, self, _ ->
                sizeExpr.generate(gen)
            },
            Operation("[]", this, int_t) to index,
            // TODO: remove
            Operation("[]", this, float_t) to index
    )

    // FIXME
    override fun declare(name: String, value: ConstantExpression?, state: CompileState?): List<Expression> {
        val sizeVal = sizeExpr.evaluate(state)
        val size = sizeVal?.let { (it.any as Number).toInt() } ?: -1
        return with(linkedListOf<Expression>()) {
            add(DeclarationExpression(name, this@array_t))
            add(DeclarationExpression("${name}_size", int_t, ConstantExpression(size)))
            add(generateAccessor(name))
            size.indices.forEachIndexed { i, _ ->
                addAll(generateComponent(name, i))
            }
            this
        }
    }

    private fun generateAccessorName(id: String) = "__${id}_access"

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
        func.addAll(listOf(ReturnStatement(
                UnaryExpression.Dereference(BinaryExpression.Add(
                        UnaryExpression.Address(
                                ReferenceExpression(func)
                        ),
                        BinaryExpression.Add(
                                ConstantExpression(1),
                                ReferenceExpression(index)
                        )
                ))
        )))
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
        return with(linkedListOf<Expression>()) {
            val field = DeclarationExpression("${accessor}_field", type)
            add(field)
            val fieldReference = ReferenceExpression(field)
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
                            ReferenceExpression(mode), true,
                            BinaryExpression.Assign(fieldReference, ReferenceExpression(value)),
                            fieldReference
                    )))
            ))
            this
        }
    }

}
