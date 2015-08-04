package com.timepath.compiler

import com.timepath.compiler.api.Backend
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.api.Frontend
import org.anarres.cpp.FileLexerSource
import org.anarres.cpp.LexerSource
import org.anarres.cpp.Source
import org.anarres.cpp.StringLexerSource
import org.antlr.v4.runtime.ParserRuleContext
import java.io.File
import java.net.URL

public class Compiler<F, B, State, AST, Out>(val frontend: F, val backend: B) :
        Frontend<State, AST> by frontend,
        Backend<State, AST, Out> by backend
where F : Frontend<State, AST>, B : Backend<State, AST, Out>, State : CompileState, Out : Any {

    fun parse() = frontend.parse(includes, state)
    fun compile() = backend.compile(parse())

    class Err(val ctx: ParserRuleContext, val reason: String) {
        private val token = ctx.start
        val file = token.getTokenSource().getSourceName()
        val line = token.getLine()
        val col = token.getCharPositionInLine()
        val code = ctx.getTextWS()
    }

    val includes: MutableList<Include> = linkedListOf()

    data class Include(
            val name: String,
            val path: String,
            val source: Source
    ) {
        companion object {
            fun invoke(input: String, name: String) = Include(name, name, StringLexerSource(input))

            fun invoke(file: File) = Include(file.name, file.canonicalPath, FileLexerSource(file))

            fun invoke(url: URL): Include {
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
        includes.add(Include(input, name))
    }

    public fun include(file: File) {
        includes.add(Include(file))
    }

    fun includeFrom(progs: File) {
        val files = progs.readLines().drop(1).map {
            val name = it.replaceFirst("//.*".toRegex(), "").trim()
            val file = File(progs.getParent(), name)
            if (name.isNotEmpty() && file.exists()) file else null
        }.filterNotNull()
        files.forEach { include(it) }
    }

}
