package com.timepath.quakec

import org.anarres.cpp.*
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.tree.ParseTree
import javax.swing.*
import java.io.File
import java.io.Reader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.Date
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.util.EnumSet
import java.util.LinkedList
import java.awt.Dimension
import com.timepath.quakec.vm.ProgramData
import com.timepath.quakec.Compiler.Include
import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.BlockStatement
import com.timepath.quakec.ast.Statement
import com.timepath.quakec.ast.impl.DeclarationExpression

public class Compiler {

    val debugThreads = true
    val debugPP = false

    val preprocessor = object : Preprocessor() {

        override fun token(): Token? {
            try {
                return super.token()
            } catch (e: Exception) {
                if (e.getMessage()!!.matches("Bad token \\[#@\\d+,\\d+\\]:\"#\"")) {
                    return Token(Token.HASH, -1, -1, "#", null)
                }
                throw e
            }
        }

        override fun pragma(name: Token?, value: MutableList<Token>?) {
            if ("noref" == name!!.getText()) return
            super.pragma(name, value)
        }
    }

    {
        val now = Date()
        preprocessor.addWarnings(EnumSet.allOf(javaClass<Warning>()))
        preprocessor.setListener(DefaultPreprocessorListener())
        preprocessor.addMacro("__DATE__", now.format("\"MMM dd yyyy\""))
        preprocessor.addMacro("__TIME__", now.format("\"hh:mm:ss\""))
    }

    fun define(name: String, value: String = "1"): Compiler {
        preprocessor.addMacro(name, value)
        return this
    }

    fun Include(file: File): Include = Include(file.name, file.canonicalPath, FileLexerSource(file))
    fun Include(input: String, name: String): Include = Include(name, name, StringLexerSource(input))

    data class Include(val name: String, val path: String, val source: Source)

    val includes = LinkedList<Include>()

    public fun include(input: String, name: String): Compiler {
        includes.add(Include(input, name))
        return this
    }

    {
        val predefs = this.javaClass.getResourceAsStream("/predefs.qc")
        includes.add(Include("predefs.qc", "<predefs>", InputLexerSource(predefs)))
    }

    public fun include(file: File): Compiler {
        includes.add(Include(file))
        return this
    }

    fun includeFrom(progs: File): Compiler {
        includes.addAll(progs.readLines().drop(1).map {
            val name = it.replaceFirst("\\s*//.*", "")
            val file = File(progs.getParent(), name)
            if (name.isNotEmpty() && file.exists()) Include(file) else null
        }.filterNotNull())
        return this
    }

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

    public fun compile(): ProgramData {
        val data = ProgramData()
        val exec = if (debugThreads)
            Executors.newSingleThreadExecutor()
        else
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), DaemonThreadFactory())
        val roots = linkedListOf<List<Statement>>()
        includes.forEach { include ->
            println(include.path)
            preprocessor.addInput(include.source)
            val stream = ANTLRInputStream(preview(CppReader(preprocessor)))
            stream.name = include.path
            val tree = parse(stream)
            val walker = ParseTreeWalker.DEFAULT
            exec.submit {
                try {
                    val listener = TreePrinterListener(rules!!)
                    walker.walk(listener, tree)
                    File("out", include.path + ".lisp").let {
                        it.getParentFile().mkdirs()
                        it.writeText(listener.toString())
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
//            exec.submit {
                try {
                    val root = tree.accept(ASTTransform())[0]
                    File("out", include.path + ".xml").let {
                        it.getParentFile().mkdirs()
                        val s = root.toStringRecursive()
                        it.writeText(s)
                    }
//                    synchronized(roots) {
                        roots.add(root.children)
//                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
//            }
        }
        exec.shutdown()
        exec.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        val ctx = GenerationContext(roots.flatMap { it })
        val asm = ctx.generate()
        File("out", "asm").let {
            it.getParentFile().mkdirs()
            asm.forEach { ir ->
                if (!ir.dummy)
                    it.appendText(ir.toString() + '\n')
            }
        }
        File("out", "unit.xml").let {
            it.getParentFile().mkdirs()

            val s = BlockStatement(includes.zip(roots).flatMap {
                listOf(DeclarationExpression("__FILE__ = ${it.first.path}")) + it.second
            }).toStringRecursive()
            it.writeText(s)
        }
        return data
    }
}

fun main(args: Array<String>) {
    val time = {(name: String, action: () -> Unit) ->
        val start = Date()
        action()
        println("$name: ${(Date().getTime() - start.getTime()).toDouble() / 1000} seconds")
    }
    val xonotic = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic"
    time("Total time") {
        val defs = linkedMapOf(
                "menu" to "MENUQC",
                "client" to "CSQC",
                "server" to "SVQC"
        )
        defs.keySet().forEach { project ->
            time("Project time") {
                Compiler()
                        .includeFrom(File("$xonotic/data/xonotic-data.pk3dir/qcsrc/$project/progs.src"))
                        .define(defs[project])
                        .compile()
            }
        }
    }
    time("GMQCC tests") {
        val gmqcc = Compiler()
                .define("GMQCC")
                .define("__STD_GMQCC__")
        gmqcc.preprocessor.addFeatures(
                Feature.DIGRAPHS,
                Feature.TRIGRAPHS
        )
        val include = {(filter: (file: File) -> Boolean) ->
            val files = File(xonotic, "gmqcc/tests").listFiles(filter)
            files?.sort()
            files?.forEach {
                gmqcc.include(it)
            }
        }
        include { it.name.endsWith(".qh") }
        include { it.name.endsWith(".qc") }
        gmqcc.compile()
    }
}