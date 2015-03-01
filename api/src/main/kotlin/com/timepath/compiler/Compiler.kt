package com.timepath.compiler

import java.awt.Dimension
import java.io.File
import java.io.Reader
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import javax.swing.*
import com.timepath.Logger
import com.timepath.compiler.api.Frontend
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.preproc.CustomPreprocessor
import org.anarres.cpp.*
import org.antlr.v4.runtime.ANTLRInputStream
import com.timepath.compiler.api.CompileState
import java.net.URL

public class Compiler(val parser: Frontend, val opts: CompilerOptions = CompilerOptions()) {

    class object {
        val logger = Logger.new()
        val debugThreads = true
        val debugPP = false
        val writeAST = false
        val writeParse = false
        fun preview(reader: Reader): Reader {
            if (debugPP) {
                val area = JTextArea()
                area.setText(reader.readText())
                val pane = JScrollPane(area)
                pane.setPreferredSize(Dimension(500, 500))
                JOptionPane.showMessageDialog(null, pane)
            }
            return reader
        }
    }

    val preprocessor = CustomPreprocessor()
    fun define(name: String, value: String = "1"): Compiler {
        preprocessor.addMacro(name, value)
        return this
    }

    {
        define("QCC_SUPPORT_INT")
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

    {
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

    val state = CompileState(opts = opts)

    fun ast(): List<List<Expression>> {
        val roots = linkedListOf<List<Expression>>()
        for (include in includes) {
            logger.info(include.path)
            preprocessor.addInput(include.source)
            val stream = ANTLRInputStream(preview(CppReader(preprocessor)))
            stream.name = include.path
            val root = parser.parse(stream, state)
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
        exec.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        return roots
    }

    public fun compile(roots: List<List<Expression>> = ast()): Any = state.gen.generate(roots.flatMap { it })
}
