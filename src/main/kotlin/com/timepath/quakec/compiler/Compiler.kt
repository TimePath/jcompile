package com.timepath.quakec.compiler

import java.awt.Dimension
import java.io.File
import java.io.Reader
import java.util.Date
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import javax.swing.*
import com.timepath.quakec.Logging
import com.timepath.quakec.QCLexer
import com.timepath.quakec.QCParser
import com.timepath.quakec.compiler.ast.*
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.preproc.CustomPreprocessor
import com.timepath.quakec.compiler.test.TreePrinterListener
import com.timepath.quakec.vm.ProgramData
import com.timepath.quakec.vm.util.IOWrapper
import com.timepath.quakec.vm.util.ProgramDataWriter
import org.anarres.cpp.*
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker

public class Compiler(val opts: CompilerOptions = CompilerOptions()) {

    class object {
        val logger = Logging.new()
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

        var rules: List<String>? = null

        fun parse(input: ANTLRInputStream): ParseTree {
            val lexer = QCLexer(input)
            val tokens = CommonTokenStream(lexer)
            val parser = QCParser(tokens)
            parser.getInterpreter().setPredictionMode(PredictionMode.SLL)
            val tree = try {
                parser.compilationUnit() // STAGE 1
            } catch (ignored: Exception) {
                // rewind input stream
                lexer.reset()
                tokens.reset()
                parser.reset()
                parser.getInterpreter().setPredictionMode(PredictionMode.LL)
                parser.compilationUnit() // STAGE 2
                // if we parse ok, it's LL not SLL
            }
            rules = parser.getRuleNames().toList()
            return tree
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

    data class Include(val name: String,
                       val path: String,
                       val source: Source)

    fun Include(input: String, name: String): Include = Include(name, name, StringLexerSource(input))
    public fun include(input: String, name: String): Compiler {
        includes.add(Include(input, name))
        return this
    }

    fun Include(file: File): Include = Include(file.name, file.canonicalPath, FileLexerSource(file))
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

    {
        val predefs = this.javaClass.getResourceAsStream("/predefs.qc")
        includes.add(Include("predefs.qc", "<predefs>", InputLexerSource(predefs)))
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
        val types = TypeRegistry()
        for (include in includes) {
            logger.info(include.path)
            preprocessor.addInput(include.source)
            val stream = ANTLRInputStream(preview(CppReader(preprocessor)))
            stream.name = include.path
            val tree = parse(stream)
            val root = tree.accept(ASTTransform(types)).single()
            roots.add(root.children)
            debug("printing parse tree", writeParse) {
                val listener = TreePrinterListener(rules!!)
                ParseTreeWalker.DEFAULT.walk(listener, tree)
                File("out", include.path + ".lisp").let {
                    it.getParentFile().mkdirs()
                    val s = listener.toString()
                    it.writeText(s)
                }
            }
            debug("printing AST", writeAST) {
                File("out", include.path + ".xml").let {
                    it.getParentFile().mkdirs()
                    val s = root.toStringRecursive()
                    it.writeText(s)
                }
            }
        }
        exec.shutdown()
        exec.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        return roots
    }

    public fun compile(roots: List<List<Expression>> = ast()): ProgramData {
        val gen = Generator(opts)
        val ir = gen.generate(roots.flatMap { it })
        return ir.generateProgs()
    }
}

val logger = Logging.new()

fun time(name: String, action: () -> Unit) {
    val start = Date()
    action()
    logger.info("$name: ${(Date().getTime() - start.getTime()).toDouble() / 1000} seconds")
}

fun main(args: Array<String>) {
    val opts = CompilerOptions()
    val xonotic = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic"
    time("Total time") {
        [data] class Project(val root: String, val define: String, val out: String)

        val defs = listOf(
                Project("menu", "MENUQC", "menuprogs.dat"),
                Project("client", "CSQC", "csprogs.dat"),
                Project("server", "SVQC", "progs.dat")
        )
        defs.forEach { project ->
            time("Project time") {
                val compiler = Compiler(opts)
                        .includeFrom(File("$xonotic/data/xonotic-data.pk3dir/qcsrc/${project.root}/progs.src"))
                        .define(project.define)
                val compiled = compiler.compile()
                ProgramDataWriter(IOWrapper.File(File("out", project.out), write = true)).write(compiled)
            }
        }
    }
    time("GMQCC tests") {
        val gmqcc = Compiler(opts)
                .define("GMQCC")
                .define("__STD_GMQCC__")
        gmqcc.preprocessor.addFeatures(
                Feature.DIGRAPHS,
                Feature.TRIGRAPHS
        )
        val include = {(filter: (file: File) -> Boolean) ->
            val files = File("$xonotic/gmqcc/tests").listFiles(filter)
            if (files != null) {
                files.sort()
                files.forEach {
                    gmqcc.include(it)
                }
            }
        }
        include { it.name.endsWith(".qh") }
        //        include { it.name.endsWith(".qc") }
        gmqcc.include(File("$xonotic/gmqcc/tests/fieldparams.qc"))
        gmqcc.compile()
    }
}
