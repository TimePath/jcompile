package com.timepath.quakec

import groovy.transform.CompileStatic
import org.anarres.cpp.*
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.Trees

import javax.annotation.Nonnull
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextArea
import java.awt.Dimension

@CompileStatic
class Compiler {

    static boolean debug = false

    static main(String[] args) {
        def now = new Date()
        def data = "${System.properties["user.home"]}/IdeaProjects/xonotic/data/xonotic-data.pk3dir"
        def defs = ['menu': 'MENUQC', 'client': 'CSQC', 'server': 'SVQC']
        for (project in defs.keySet()) {
            def pp = createPreprocessor(now)
            pp.addMacro(defs[project])
            def includes = includeAll("${data}/qcsrc/${project}/progs.src" as File)
            for (File file in includes) {
                pp.addInput(new FileLexerSource(file))
                parse(pp, file)
            }
            pp.macros[defs[project]] = (Macro) null
        }
    }

    static Preprocessor createPreprocessor(Date now = new Date()) {
        def pp = new Preprocessor() {
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
                if ("noref".equals(name.text)) {
                    return
                }
                super.pragma(name, value)
            }
        }

        pp.addWarnings(EnumSet.allOf(Warning))

        pp.listener = new DefaultPreprocessorListener()

        pp.addMacro("__JCPP__")
        pp.addMacro("__DATE__", now.format('"MMM dd yyyy"'))
        pp.addMacro("__TIME__", now.format('"hh:mm:ss"'))
        return pp
    }

    static LinkedList<File> includeAll(File progs) {
        LinkedList<File> includes = []
        for (line in progs.readLines().drop(1)) {
            def name = line.replaceFirst($/\s*//.*/$, '')
            def file = new File(progs.parent, name)
            if (name && file.exists()) includes.add(file)
        }
        return includes
    }

    static Reader preview(Reader reader) {
        if (debug) {
            def area = new JTextArea()
            area.text = reader.readLines().join('\n')
            def pane = new JScrollPane(area)
            pane.setPreferredSize([500, 500] as Dimension)
            JOptionPane.showMessageDialog(null, pane)
        }
        return reader
    }

    static def parse(Preprocessor pp, File f) {
        println f.absolutePath
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
        new File('out', f.canonicalPath).with {
            parentFile.mkdirs()
            def listener = new TreePrinterListener(parser);
            ParseTreeWalker.DEFAULT.walk(listener, tree);
            def formatted = listener.toString();
            text = formatted
        }
    }

}