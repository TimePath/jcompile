package com.timepath.compiler.ast

import kotlin.properties.Delegates
import com.timepath.compiler.Type
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import com.timepath.q1vm.Instruction
import org.antlr.v4.runtime.ParserRuleContext

class MethodCallExpression(val function: Expression,
                           add: List<Expression>? = null,
                           ctx: ParserRuleContext? = null) : Expression(ctx) {

    {
        if (add != null) {
            addAll(add)
        }
    }

    override fun type(gen: Generator): Type = (function.type(gen) as Type.Function).type

    val args: List<Expression> by Delegates.lazy {
        children.filterIsInstance<Expression>()
    }

    override val attributes: Map<String, Any?>
        get() = mapOf("id" to function)

    override fun toString(): String = "$function(${args.joinToString(", ")})"

    override fun generate(gen: Generator): List<IR> {
        // TODO: increase this
        if (args.size() > 8) {
            Generator.logger.warning("${function} takes ${args.size()} parameters")
        }
        val args = args.take(8).map { it.doGenerate(gen) }
        fun instr(i: Int) = Instruction.from(Instruction.CALL0.ordinal() + i)
        var i = 0
        val prepare: List<IR> = args.map {
            val param = Instruction.OFS_PARAM(i++)
            IR(Instruction.STORE_FLOAT, array(it.last().ret, param), param, "Prepare param $i")
        }
        val global = gen.allocator.allocateReference(type = type(gen))
        with(linkedListOf<IR>()) {
            val genF = function.doGenerate(gen)
            addAll(genF)
            addAll(args.flatMap { it })
            addAll(prepare)
            add(IR(instr(i), array(genF.last().ret), Instruction.OFS_PARAM(-1)))
            add(IR(Instruction.STORE_FLOAT, array(Instruction.OFS_PARAM(-1), global.ref), global.ref, "Save response"))
            return this
        }
    }
}
