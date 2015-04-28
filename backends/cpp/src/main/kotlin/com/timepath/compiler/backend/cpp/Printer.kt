package com.timepath.compiler.backend.cpp

public class Printer private(private val indent: String) {
    private val lines: MutableList<String> = linkedListOf()
    override fun toString() = lines.sequence().map { indent + it }.join("\n")
    fun T.plus<T>() = this@Printer.lines.addAll(this@plus.toString().split('\n')).let { Unit }
    inline fun String.invoke(body: Printer.() -> Unit) = Printer(this, body)

    fun terminate(s: String): Printer {
        lines[lines.lastIndex] += s
        return this
    }

    companion object {
        inline fun invoke(indent: String = "", body: Printer.() -> Unit) = Printer(indent).let {
            it.body()
            it
        }

        fun invoke(body: String = "") = Printer("") { +body }
    }
}
