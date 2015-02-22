package com.timepath.compiler.types

import com.timepath.compiler.ast.Expression
import com.timepath.compiler.ast.MemberExpression
import com.timepath.compiler.ast.ReferenceExpression
import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.IndexExpression
import com.timepath.compiler.gen.generate
import com.timepath.compiler.ast.MethodCallExpression
import com.timepath.compiler.gen.evaluate
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.ast.FunctionExpression
import com.timepath.compiler.ast.ParameterExpression
import com.timepath.compiler.ast.UnaryExpression
import com.timepath.compiler.ast.BinaryExpression
import com.timepath.compiler.ast.ConditionalExpression
import com.timepath.compiler.ast.ReturnStatement
import com.timepath.compiler.api.CompileState

data class array_t(val type: Type, val sizeExpr: Expression) : pointer_t() {

    override fun toString() = "$type[$sizeExpr]"

    val index = OperationHandler(type) { gen, left, right ->
        when (left) {
            is MemberExpression -> {
                val field = ReferenceExpression((left.right as ConstantExpression).value.any as String)
                IndexExpression(left.left, IndexExpression(field, right!!)).generate(gen)
            }
            is ReferenceExpression -> {
                val s = generateAccessorName(left.id)
                val indexer = MethodCallExpression(ReferenceExpression(s), listOf(right!!))
                MethodCallExpression(indexer, listOf(ConstantExpression(0))).generate(gen)
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
        val size = (sizeExpr.evaluate(state)!!.any as Number).toInt()
        val intRange = size.indices
        return with(linkedListOf<Expression>()) {
            add(DeclarationExpression(name, this@array_t))
            add(DeclarationExpression("${name}_size", int_t, ConstantExpression(size.toFloat())))
            add(generateAccessor(name))
            intRange.forEachIndexed {(i, _) ->
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
        return FunctionExpression(
                accessor,
                function_t(function_t(float_t, listOf(float_t, float_t), null), listOf(float_t), null),
                listOf(ParameterExpression("index", float_t, 0)),
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
    private fun generateComponent(id: String, i: Int): List<Expression> {
        val accessor = "${generateAccessorName(id)}_${i}"
        val field = "${accessor}_field"
        return with(linkedListOf<Expression>()) {
            add(DeclarationExpression(field, type))
            val fieldReference = ReferenceExpression(field)
            add(FunctionExpression(
                    accessor,
                    function_t(float_t, listOf(float_t, float_t), null),
                    listOf(
                            ParameterExpression("mode", float_t, 0),
                            ParameterExpression("value", float_t, 1)
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

}
