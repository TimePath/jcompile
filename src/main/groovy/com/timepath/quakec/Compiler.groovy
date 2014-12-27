package com.timepath.quakec

import groovy.transform.CompileStatic
import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode

@CompileStatic
class Compiler {

    static main(String[] args) {
        println 'begin'
        CharStream input = new ANTLRFileStream('hello.qc');
        QCLexer lexer = new QCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        QCParser parser = new QCParser(tokens);
        parser.interpreter.predictionMode = PredictionMode.SLL;
        try {
            parser.compilationUnit();  // STAGE 1
        } catch (ignored) {
            tokens.reset(); // rewind input stream
            parser.reset();
            parser.interpreter.predictionMode = PredictionMode.LL;
            parser.compilationUnit();  // STAGE 2
            // if we parse ok, it's LL not SLL
        }
        println 'end'
    }

}