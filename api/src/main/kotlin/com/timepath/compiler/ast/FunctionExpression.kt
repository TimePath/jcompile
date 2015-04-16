package com.timepath.compiler.ast

import com.timepath.compiler.types.defaults.function_t
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Replaced with a number during compilation
 */
class FunctionExpression(id: String? = null,
                         type: function_t,
                         val params: List<Expression>? = null,
                         val vararg: Expression? = null,
                         add: List<Expression>? = null,
                         val builtin: Int? = null,
                         override val ctx: ParserRuleContext? = null) : DeclarationExpression(id ?: "func", type) {

    init {
        if (add != null) {
            addAll(add)
        }
    }

    override val simpleName = "FunctionExpression"
    override fun <T> accept(visitor: ASTVisitor<T>) = visitor.visit(this)

}
