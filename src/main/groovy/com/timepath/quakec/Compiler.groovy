package com.timepath.quakec

import com.timepath.quakec.util.DaemonThreadFactory
import com.timepath.quakec.vm.defs.ProgramData
import groovy.transform.CompileStatic
import org.anarres.cpp.*
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker

import javax.annotation.Nonnull
import javax.swing.*
import java.awt.*
import java.util.List
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@CompileStatic
class Compiler {

    static boolean debugThreads = true
    static boolean debugPP = false

    static void main(String[] args) {
        def start = new Date()
        def data = "${System.properties["user.home"]}/IdeaProjects/xonotic/data/xonotic-data.pk3dir"
        def defs = ['menu': 'MENUQC', 'client': 'CSQC', 'server': 'SVQC']
        for (project in defs.keySet()) {
            def part = new Date()
            new Compiler()
                    .include("${data}/qcsrc/${project}/progs.src" as File)
                    .define(defs[project])
                    .compile()
            println "Project time: ${(new Date().time - part.time) / 1000} seconds"
        }
        println "Total time: ${(new Date().time - start.time) / 1000} seconds"
    }

    private static Collection<Include> includeAll(File progs) {
        def files = progs.readLines().drop(1)
        files.findResults {
            def name = it.replaceFirst($/\s*//.*/$, '')
            def file = new File(progs.parent, name)
            (name && file.exists()) ? new Include(file) : null
        }
    }

    private static Reader preview(Reader reader) {
        if (debugPP) {
            def area = new JTextArea()
            area.text = reader.readLines().join('\n')
            def pane = new JScrollPane(area)
            pane.setPreferredSize([500, 500] as Dimension)
            JOptionPane.showMessageDialog(null, pane)
        }
        return reader
    }

    private static QCParser parser

    private static ParseTree parse(ANTLRInputStream input) {
        QCLexer lexer = new QCLexer(input)
        CommonTokenStream tokens = new CommonTokenStream(lexer)
        parser = new QCParser(tokens)
        parser.interpreter.predictionMode = PredictionMode.SLL
        def tree
        try {
            tree = parser.compilationUnit()  // STAGE 1
        } catch (ignored) {
            tokens.reset() // rewind input stream
            parser.reset()
            parser.interpreter.predictionMode = PredictionMode.LL
            tree = parser.compilationUnit()  // STAGE 2
            // if we parse ok, it's LL not SLL
        }
        return tree
    }

    private Preprocessor preprocessor
    private List<Include> includes = []

    Compiler() {
        def now = new Date()
        preprocessor = new Preprocessor() {
            @Override
            Token token() throws IOException, LexerException {
                def t
                try {
                    t = super.token()
                } catch (e) {
                    def matcher = e.message =~ /Bad token \[#@\d+,\d+\]:"#"/
                    if (matcher.matches())
                        t = new Token(Token.HASH, -1, -1, '#', null)
                    else throw e
                }
                return t
            }

            @Override
            protected void pragma(@Nonnull Token name, @Nonnull List<Token> value) throws IOException, LexerException {
                if ("noref".equals(name.text)) return
                super.pragma(name, value)
            }
        }
        preprocessor.addWarnings(EnumSet.allOf(Warning))
        preprocessor.listener = new DefaultPreprocessorListener()
        preprocessor.addMacro("__DATE__", now.format('"MMM dd yyyy"'))
        preprocessor.addMacro("__TIME__", now.format('"hh:mm:ss"'))
    }

    Compiler include(File progs) {
        includes.addAll(includeAll(progs))
        this
    }

    Compiler include(String input, String name = "-") {
        includes << new Include(input, name)
        this
    }

    Compiler define(String name, String value = "1") {
        preprocessor.addMacro(name, value)
        this
    }

    ProgramData compile() {
        def data = new ProgramData()
        def exec = debugThreads ? Executors.newSingleThreadExecutor()
                : Executors.newFixedThreadPool(Runtime.runtime.availableProcessors(), new DaemonThreadFactory());
        for (Include include in includes) {
            println include.path
            preprocessor.addInput(include.source)
            def stream = new ANTLRInputStream(preview(new CppReader(preprocessor)))
            stream.name = include.name
            def tree = parse(stream)
            def walker = ParseTreeWalker.DEFAULT
            exec.submit {
                def listener = new TreePrinterListener(parser);
                walker.walk(listener, tree);
                def formatted = listener.toString();
                new File('out', include.path).with {
                    parentFile.mkdirs()
                    text = formatted
                }
            }
            exec.submit {
                def listener = new ScopeCollector();
                walker.walk(listener, tree);
            }
        }
        exec.shutdown()
        exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        return data
    }

}