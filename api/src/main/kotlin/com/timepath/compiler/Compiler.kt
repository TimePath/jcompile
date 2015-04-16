package com.timepath.compiler

import com.timepath.DaemonThreadFactory
import com.timepath.Logger
import com.timepath.compiler.api.Backend
import com.timepath.compiler.api.Frontend
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.test.PrintVisitor
import org.anarres.cpp.FileLexerSource
import org.anarres.cpp.LexerSource
import org.anarres.cpp.Source
import org.anarres.cpp.StringLexerSource
import java.io.File
import java.net.URL
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level

public class Compiler(val parser: Frontend, val backend: Backend = Backend.Null) {

    val state = backend.state

    companion object {
        val logger = Logger.new()
        val debugThreads = true
        val writeAST = false
        val writeParse = false
    }

    fun define(name: String, value: String = "1"): Compiler {
        parser.define(name, value)
        return this
    }

    init {
        // define("QCC_SUPPORT_INT")
        define("QCC_SUPPORT_BOOL")
    }

    val includes = LinkedList<Include>()

    data trait Include {
        val name: String
        val path: String
        val source: Source
    }

    fun Include(input: String, name: String): Include = object : Include {
        override val name = name
        override val path = name
        override val source: Source
            get() = StringLexerSource(input)
    }

    public fun include(input: String, name: String): Compiler {
        includes.add(Include(input, name))
        return this
    }

    fun Include(file: File): Include = object : Include {
        override val name = file.name
        override val path = file.canonicalPath
        override val source: Source
            get() = FileLexerSource(file)
    }

    public fun include(file: File): Compiler {
        includes.add(Include(file))
        return this
    }

    fun includeFrom(progs: File): Compiler {
        progs.readLines().drop(1).map {
            val name = it.replaceFirst("//.*", "").trim()
            val file = File(progs.getParent(), name)
            if (name.isNotEmpty() && file.exists()) Include(file) else null
        }.filterNotNullTo(includes)
        return this
    }

    fun Include(url: URL): Include = object : Include {
        override val name = url.getPath().substringAfterLast('/')
        override val path = url.getPath()
        override val source: Source
            get() = object : LexerSource(url.openStream().buffered().reader(), true) {
                override fun getName() = name
                override fun getPath() = path
            }
    }

    init {
        includes.add(Include(this.javaClass.getResource("/predefs.qc")))
    }

    val exec = if (debugThreads)
        Executors.newSingleThreadExecutor()
    else
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), DaemonThreadFactory())

    inline fun debug(s: String, predicate: Boolean, inlineOptions(InlineOption.ONLY_LOCAL_RETURN) action: () -> Unit) {
        if (predicate) {
            exec.submit {
                try {
                    action()
                } catch (e: Throwable) {
                    logger.log(Level.SEVERE, "Error $s", e)
                }
            }
        }
    }

    fun ast(): List<List<Expression>> {
        val roots = linkedListOf<List<Expression>>()
        for (include in includes) {
            logger.info(include.path)
            val root = parser.parse(include, state)
            roots.add(root.children)
            //            debug("printing parse tree", writeParse) {
            //                val listener = TreePrinterListener(rules!!)
            //                ParseTreeWalker.DEFAULT.walk(listener, tree)
            //                File("out", include.path + ".lisp").let {
            //                    it.getParentFile().mkdirs()
            //                    val s = listener.toString()
            //                    it.writeText(s)
            //                }
            //            }
            debug("printing AST", writeAST) {
                File("out", include.path + ".xml").let {
                    it.getParentFile().mkdirs()
                    val s = PrintVisitor.render(root)
                    it.writeText(s)
                }
            }
        }
        exec.shutdown()
        exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        return roots
    }

    public fun compile(roots: List<List<Expression>> = ast()): Any = backend.generate(roots.flatMap { it })
}
