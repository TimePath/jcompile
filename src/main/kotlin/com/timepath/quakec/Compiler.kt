package com.timepath.quakec

import com.timepath.quakec.vm.defs.ProgramData
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

    val includes = LinkedList<Include>()

    public fun include(input: String, name: String): Compiler {
        includes.add(Include(input, name))
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
        includes.forEach { include ->
            println(include.path)
            preprocessor.addInput(include.source)
            val stream = ANTLRInputStream(preview(CppReader(preprocessor)))
            stream.name = include.name
            val tree = parse(stream)
            val walker = ParseTreeWalker.DEFAULT
            exec.submit {
                val listener = TreePrinterListener(rules!!)
                walker.walk(listener, tree)
                File("out", include.path).let {
                    it.getParentFile().mkdirs()
                    it.writeText(listener.toString())
                }
            }
            exec.submit {
                val listener = ScopeCollector()
                walker.walk(listener, tree)
            }
        }
        exec.shutdown()
        exec.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        return data
    }
}

fun main(args: Array<String>) {
    val start = Date()
    val data = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic/data/xonotic-data.pk3dir"
    val defs = linkedMapOf(
            "menu" to "MENUQC",
            "client" to "CSQC",
            "server" to "SVQC"
    )
    defs.keySet().forEach { project ->
        val part = Date()
        Compiler()
                .includeFrom(File("$data/qcsrc/$project/progs.src"))
                .define(defs[project])
                .compile()
        println("Project time: ${(Date().getTime() - part.getTime()).toDouble() / 1000} seconds")
    }
    println("Total time: ${(Date().getTime() - start.getTime()).toDouble() / 1000} seconds")
}