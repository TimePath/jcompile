package com.timepath.quakec.compiler.ast

import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR

abstract class Statement {

    open val attributes: Map<String, Any?>
        get() = mapOf()

    private val mutableChildren: MutableList<Statement> = linkedListOf()

    val children: List<Statement>
        get() = mutableChildren

    fun initChild<T : Statement>(elem: T, configure: (T.() -> Unit)? = null): T {
        if (configure != null)
            elem.configure()
        add(elem)
        return elem
    }

    fun add(s: Statement) {
        mutableChildren.add(s)
    }

    fun addAll(c: List<Statement>) {
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

    open fun generate(ctx: Generator): List<IR> = emptyList()

}

abstract class Expression : Statement() {
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
class Nop : Statement()

class BlockStatement(c: List<Statement>? = null) : Statement() {
    {
        if (c != null) {
            addAll(c)
        }
    }
    override fun generate(ctx: Generator): List<IR> {
        ctx.allocator.push("<block>")
        val list = children.flatMap {
            it.generate(ctx)
        }
        ctx.allocator.pop()
        return list
    }
}
