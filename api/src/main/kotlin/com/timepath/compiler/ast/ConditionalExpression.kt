package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.Value
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.compiler.gen.ReferenceIR
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

class ConditionalExpression(val test: Expression,
                            val expression: Boolean,
                            val pass: Expression,
                            val fail: Expression? = null,
                            ctx: ParserRuleContext? = null) : Expression(ctx) {

    {
        add(test)
        add(pass)
        if (fail != null) {
            add(fail)
        }
    }

    override fun type(gen: Generator): Type {
        val type = pass.type(gen)
        return when (fail) {
            null -> Type.Void // Type.Nullable(type)
            else -> type
        }
    }

    override fun evaluate(): Value? {
        val result = test.evaluate()
        if (result == null) return null
        val eval = @lambda {(it: Expression?): Value? ->
            return@lambda if (it is Expression) it.evaluate() else null
        }
        return if (result.toBoolean()) eval(pass) else eval(fail)
    }

    override fun toString(): String = "($test ? $pass : $fail)"

}
