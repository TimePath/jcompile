package com.timepath.compiler.ast

import com.timepath.compiler.types.defaults.function_t
import org.antlr.v4.runtime.ParserRuleContext as PRC

/**
 * Replaced with a number during compilation
 */
public class FunctionExpression(id: String? = null,
                                override val type: function_t,
                                val params: List<ParameterExpression>? = null,
                                val vararg: Expression? = null,
                                add: List<Expression>? = null,
                                val builtin: Int? = null,
                                override val ctx: PRC? = null) : DeclarationExpression(id ?: "func", type) {

    override fun withChildren(children: List<Expression>) = copy(children = children)

    fun copy(
            id: String = this.id,
            type: function_t = this.type,
            params: List<ParameterExpression>? = this.params,
            vararg: Expression? = this.vararg,
            children: List<Expression>? = this.children,
            builtin: Int? = this.builtin,
            ctx: PRC? = this.ctx
    ) = FunctionExpression(id, type, params, vararg, children, builtin, ctx)

    init {
        add?.let { addAll(it) }
    }

    override val simpleName = "FunctionExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

}
