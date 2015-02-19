package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.gen.FunctionIR
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.compiler.gen.LabelIR
import com.timepath.compiler.gen.ReferenceIR
import com.timepath.q1vm.Function
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Replaced with a number during compilation
 */
class FunctionExpression(val id: String? = null,
                         val signature: Type.Function,
                         val params: List<Expression>? = null,
                         val vararg: Expression? = null,
                         add: List<Expression>? = null,
                         val builtin: Int? = null,
                         ctx: ParserRuleContext? = null) : Expression(ctx) {

    {
        if (add != null) {
            addAll(add)
        }
    }

    override fun type(gen: Generator) = signature

}
