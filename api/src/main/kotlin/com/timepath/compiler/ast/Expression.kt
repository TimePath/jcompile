package com.timepath.compiler.ast

import com.timepath.compiler.Type
import com.timepath.compiler.Value
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.gen.IR
import org.antlr.v4.runtime.ParserRuleContext

abstract class Expression(val ctx: ParserRuleContext? = null) {

    abstract fun type(gen: Generator): Type

    fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visit(this)

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

    fun doGenerate(gen: Generator): List<IR> {
        try {
            // TODO: push up
            return GeneratorVisitor(gen).visit(this)
        } catch(t: Throwable) {
            val rule: ParserRuleContext? = this.ctx
            if (rule != null) {
                val source = rule.start.getTokenSource()
                println("E: ${source.getSourceName()}:${source.getLine()}:${source.getCharPositionInLine()}")
                println("E: ${rule.getText()}")
            }
            throw t
        }
    }

    /**
     * Used in constant folding
     *
     * @return A constant or null if it could change at runtime
     */
    open fun evaluate(): Value? = throw UnsupportedOperationException()

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
class Nop(ctx: ParserRuleContext? = null) : Expression(ctx) {
    override fun type(gen: Generator): Type = throw UnsupportedOperationException()
}

