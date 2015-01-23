package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import org.antlr.v4.runtime.ParserRuleContext
import com.timepath.quakec.compiler.Value

abstract class Expression(val ctx: ParserRuleContext? = null) {

    fun transform(transform: (Expression) -> Expression?): List<Expression> {
        // TODO: pure
        val ret = mutableChildren
        val it = ret.listIterator()
        while (it.hasNext()) {
            val before = it.next()
            val after = transform(before)
            if (after == null) {
                it.remove()
            } else {
                after.transform(transform)
                it.set(after)
            }
        }
        return ret
    }

    open fun reduce(): Expression? = this

    open val attributes: Map<String, Any?>
        get() = mapOf()

    private val mutableChildren: MutableList<Expression> = linkedListOf()

    val children: List<Expression>
        get() = mutableChildren

    fun initChild<T : Expression>(elem: T, configure: (T.() -> Unit)? = null): T {
        if (configure != null)
            elem.configure()
        add(elem)
        return elem
    }

    fun add(s: Expression) {
        mutableChildren.add(s)
    }

    fun addAll(c: List<Expression>) {
        mutableChildren.addAll(c)
    }

    private fun render(sb: StringBuilder = StringBuilder(), indent: String = ""): StringBuilder {
        val name = this.javaClass.getSimpleName()
        sb.append("${indent}<${name}")
        for ((k, v) in attributes) {
            sb.append(" ${k}=\"${v.toString()
                    .replace("&", "&amp;")
                    .replace("\"", "&quot;")}\"")
        }
        if (children.isEmpty()) {
            sb.append("/>\n")
        } else {
            sb.append(">\n")
            val nextIndent = indent + "\t"
            for (c in children) {
                c.render(sb, nextIndent)
            }
            sb.append("${indent}</${name}>\n")
        }
        return sb
    }

    fun toStringRecursive(): String = render().toString()

    fun doGenerate(ctx: Generator): List<IR> {
        try {
            return generate(ctx)
        } catch(t: Throwable) {
            val rule: ParserRuleContext? = this.ctx
            if (rule != null) {
                val source = rule.start.getTokenSource()
                println("${source.getSourceName()}:${source.getLine()}:${source.getCharPositionInLine()}")
                println("${rule.getText()}")
            }
            throw t
        }
    }

    open fun generate(ctx: Generator): List<IR> = emptyList()

    /**
     * Used in constant folding
     *
     * @return A constant or null if it could change at runtime
     */
    open fun evaluate(): Value? = null

    /**
     * Used in constant folding
     *
     * @return true if constant folding is forbidden for this node (not descendants)
     */
    open fun hasSideEffects(): Boolean = false

}

/**
 * Lonely semicolon
 */
class Nop(ctx: ParserRuleContext? = null) : Expression(ctx)

