package com.timepath.compiler.backend.q1vm.types

import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.*
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.ir.IR
import com.timepath.compiler.types.Operation
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.pointer_t

data class array_t(val type: Type, val sizeExpr: Expression, val state: CompileState) : pointer_t() {

    override val simpleName = "array_t"
    override fun toString() = "$type[$sizeExpr]"

    override fun handle(op: Operation) = ops[op]
    val ops = mapOf(
            Operation("sizeof", this) to Operation.Handler.Unary<Q1VM.State, List<IR>>(int_t) {
                sizeExpr.generate()
            },
            Operation("[]", this, int_t) to Operation.Handler.Binary<Q1VM.State, List<IR>>(type) { l, r ->
                if (l !is ReferenceExpression) throw UnsupportedOperationException("cannot index non-reference array")
                // arr[i] -> arr(i)(false)
                val s = generateAccessorName(l.refers.id)
                val resolve = state.symbols[s]
                        ?: throw RuntimeException("Can't resolve $s")
                val accessor = resolve.ref()
                val indexer = MethodCallExpression(accessor, listOf(r))
                MethodCallExpression(indexer, listOf(false.expr())).generate()
            }
    )

    override fun declare(name: String, value: ConstantExpression?)
            = DeclarationExpression(name, this)

    fun generateAccessorName(id: String) = "__${id}_access"

}
