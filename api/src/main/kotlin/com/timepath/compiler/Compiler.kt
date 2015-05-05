package com.timepath.compiler

import com.timepath.compiler.api.Backend
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.api.Frontend
import com.timepath.compiler.ast.Expression
import org.anarres.cpp.FileLexerSource
import org.anarres.cpp.LexerSource
import org.anarres.cpp.Source
import org.anarres.cpp.StringLexerSource
import java.io.File
import java.net.URL
import java.util.LinkedList

public class Compiler<F : Frontend<S>, S : CompileState, O : Any, B : Backend<S, O>>(val parser: F, val backend: B) {

    val state = backend.state

    val includes = LinkedList<Include>()

    init {
        define("QCC_SUPPORT_INT")
        define("QCC_SUPPORT_BOOL")
        define("QCC_SUPPORT_ENTITYCLASS")

        includes.add(Include.new(javaClass.getResource("/predefs.qc")))
    }

    fun define(name: String, value: String = "1") {
        parser.define(name, value)
    }

    data class Include(
            val name: String,
            val path: String,
            val source: Source
    ) {
        companion object {
            fun new(input: String, name: String) = Include(name, name, StringLexerSource(input))

            fun new(file: File) = Include(file.name, file.canonicalPath, FileLexerSource(file))

            fun new(url: URL): Include {
                val name = url.getPath().substringAfterLast('/')
                val path = url.getPath()
                return Include(name, path, object : LexerSource(url.openStream().buffered().reader(), true) {
                    override fun getName() = name
                    override fun getPath() = path
                })
            }
        }
    }

    public fun include(input: String, name: String) {
        includes.add(Include.new(input, name))
    }

    public fun include(file: File) {
        includes.add(Include.new(file))
    }

    fun includeFrom(progs: File) {
        progs.readLines().drop(1).map {
            val name = it.replaceFirst("//.*", "").trim()
            val file = File(progs.getParent(), name)
            if (name.isNotEmpty() && file.exists()) Include.new(file) else null
        }.filterNotNullTo(includes)
    }

    fun ast() = parser.parse(includes, state)

    public fun compile(roots: Sequence<List<Expression>> = ast()): O = backend.generate(roots)
}
