package com.timepath.quakec

import groovy.transform.CompileStatic
import org.anarres.cpp.*
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.tree.ParseTreeWalker

import javax.annotation.Nonnull
import javax.swing.*
import java.awt.*
import java.util.List
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

@CompileStatic
class Compiler {

    static void main(String[] args) {
        def start = new Date()
        def data = "${System.properties["user.home"]}/IdeaProjects/xonotic/data/xonotic-data.pk3dir"
        def defs = ['menu': 'MENUQC', 'client': 'CSQC', 'server': 'SVQC']
        for (project in defs.keySet()) {
            new Compiler().compile("${data}/qcsrc/${project}/progs.src" as File, defs[project])
        }
        println "Total time: ${(new Date().time - start.time) / 1000} seconds"
    }

    private static Preprocessor createPreprocessor(Date now = new Date()) {
        new Preprocessor() {
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
        }.with {
            it.addWarnings(EnumSet.allOf(Warning))
            it.listener = new DefaultPreprocessorListener()
            it.addMacro("__JCPP__")
            it.addMacro("__DATE__", now.format('"MMM dd yyyy"'))
            it.addMacro("__TIME__", now.format('"hh:mm:ss"'))
            it
        }
    }

    private static Collection<File> includeAll(File progs) {
        progs.readLines().drop(1).findResults {
            def name = it.replaceFirst($/\s*//.*/$, '')
            def file = new File(progs.parent, name)
            (name && file.exists()) ? file : null
        }
    }

    boolean debug = false

    private ThreadFactory threadFactory = new ThreadFactory() {
        private ThreadFactory delegate = Executors.defaultThreadFactory()

        @Override
        Thread newThread(Runnable r) { this.delegate.newThread(r).with { daemon = true; it } }
    }

    private ExecutorService pool = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors(), threadFactory)

    def compile(File progs, String define) {
        def start = new Date()
        def pp = createPreprocessor(start)
        pp.addMacro(define)
        def includes = includeAll(progs)
        for (File file in includes) {
            pp.addInput(new FileLexerSource(file))
            parse(pp, file)
        }
        pool.shutdown()
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        println "Project time: ${(new Date().time - start.time) / 1000} seconds"
    }

    private parse(Preprocessor pp, File f) {
        def input = new ANTLRInputStream(preview(new CppReader(pp)))
        input.name = f.name
        QCLexer lexer = new QCLexer(input)
        CommonTokenStream tokens = new CommonTokenStream(lexer)
        QCParser parser = new QCParser(tokens)
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
        pool.submit {
            println f.canonicalPath
            def listener = new TreePrinterListener(parser);
            ParseTreeWalker.DEFAULT.walk(listener, tree);
            def formatted = listener.toString();
            new File('out', f.canonicalPath).with {
                parentFile.mkdirs()
                text = formatted
            }
        }
    }

    private Reader preview(Reader reader) {
        if (this.debug) {
            def area = new JTextArea()
            area.text = reader.readLines().join('\n')
            def pane = new JScrollPane(area)
            pane.setPreferredSize([500, 500] as Dimension)
            JOptionPane.showMessageDialog(null, pane)
        }
        return reader
    }

}