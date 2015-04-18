package com.timepath.compiler.ast

import com.timepath.compiler.types.defaults.function_t
import org.antlr.v4.runtime.ParserRuleContext as PRC

/**
 * Replaced with a number during compilation
 */
public class FunctionExpression(id: String? = null,
                                type: function_t,
                                val params: List<Expression>? = null,
                                val vararg: Expression? = null,
                                add: List<Expression>? = null,
                                val builtin: Int? = null,
                                override val ctx: PRC? = null) : DeclarationExpression(id ?: "func", type) {

    init {
        add?.let { addAll(it) }
    }

    override val simpleName = "FunctionExpression"
    override fun accept<T>(visitor: ASTVisitor<T>) = visitor.visit(this)

}
