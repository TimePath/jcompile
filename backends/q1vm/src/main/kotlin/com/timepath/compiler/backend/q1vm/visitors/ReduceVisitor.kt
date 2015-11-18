package com.timepath.compiler.backend.q1vm.visitors

import com.timepath.compiler.ast.*
import com.timepath.compiler.ast.SwitchExpression.Case
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.evaluate
import com.timepath.compiler.backend.q1vm.types.array_t
import com.timepath.compiler.backend.q1vm.types.bool_t
import com.timepath.compiler.backend.q1vm.types.int_t
import com.timepath.compiler.types.defaults.function_t
import java.util.concurrent.atomic.AtomicInteger

class ReduceVisitor(val state: Q1VM.State) : ASTVisitor<List<Expression>> {

    @Suppress("NOTHING_TO_INLINE") inline fun Expression.reduce() = accept(this@ReduceVisitor)

    override fun default(e: Expression) = listOf(e)

    override fun visit(e: BlockExpression) = listOf(e.withChildren(e.children.flatMap { it.reduce() }))

    override fun visit(e: FunctionExpression) = listOf(e.withChildren(e.children.flatMap { it.reduce() }))

    val uid = AtomicInteger()

    fun List<Expression>.transform(f: (Expression) -> List<Expression>?) = flatMap { it.transform(f) ?: emptyList() }

    inline fun Expression.transform(crossinline f: (Expression) -> List<Expression>?
    ): List<Expression>? {
        val transformed = f(this) ?: return null
        return transformed.map { it.withChildren(it.children.transform { f(it) }) }
    }

    override fun visit(e: SwitchExpression): List<Expression> {
        val jumps = linkedListOf<Expression>()
        val default = linkedListOf<Expression>()
        val cases = LoopExpression(checkBefore = false, predicate = 0.expr(), body = BlockExpression(e.children.transform {
            when (it) {
                is SwitchExpression ->
                    it.reduce()
                is Case -> {
                    val expr = it.expr
                    fun String.sanitizeLabel(): String = "__switch_${uid.andIncrement}_${replace("[^a-zA-Z_0-9]".toRegex(), "_")}"
                    val label = (if (expr == null) "default" else "case $expr").sanitizeLabel()
                    val goto = GotoExpression(label)
                    if (expr == null) {
                        default.add(goto)
                    } else {
                        val test = e.test eq expr
                        val jump = ConditionalExpression(test, false, goto)
                        jumps.add(jump)
                    }
                    listOf(LabelExpression(label)) // replace with a label so goto will be filled in later
                }
                else -> listOf(it)
            }
        }))
        return listOf(BlockExpression(jumps + default + cases))
    }

    override fun visit(e: DeclarationExpression): List<Expression> {
        val type = e.type
        if (type !is array_t) {
            return super.visit(e)
        }
        val sizeVal = state.let { type.sizeExpr.evaluate(it) }
        val size = sizeVal?.let { (it.any as Number).toInt() } ?: -1

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
        fun generateAccessor(id: String): Expression {
            val accessor = type.generateAccessorName(id)
            val index = ParameterExpression("index", int_t, 0)
            val func = FunctionExpression(
                    accessor,
                    function_t(function_t(type, listOf(bool_t, type), null), listOf(int_t), null),
                    listOf(index)
            )
            val ret = (func.ref().address()
                    + 1.expr()
                    + index.ref()
                    ).deref()
            func.add(ReturnStatement(ret))
            return func
        }

        /**
         *  #define ARRAY_COMPONENT(name, i, v)                         \
         *      float name##_##i = v;                                   \
         *      float name##_access_##i(bool mode, float value##i) {    \
         *          return mode ? name##_##i = value##i : name##_##i;   \
         *      }
         */
        fun generateComponent(id: String, i: Int): List<Expression> {
            val accessor = "${type.generateAccessorName(id)}_$i"
            return linkedListOf<Expression>().apply {
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
        return linkedListOf<Expression>().apply {
            val name = e.id
            add(DeclarationExpression(name, type).apply {
                state.symbols.declare(this)
            })
            add(DeclarationExpression("${name}_size", int_t, size.expr()).apply {
                state.symbols.declare(this)
            })
            add(generateAccessor(name).apply {
                state.symbols.declare(this)
            })
            repeat(size) {
                addAll(generateComponent(name, it).apply {
                    state.symbols.declare(this)
                })
            }
        }
    }

    override fun visit(e: LoopExpression): List<Expression> {
        // do <expr> while (false)
        if (e.checkBefore == false && e.predicate.evaluate(state)?.toBoolean() == false) {
            return e.children
        }
        return listOf(e)
    }

    override fun visit(e: UnaryExpression.Cast) = e.operand.reduce()
}
