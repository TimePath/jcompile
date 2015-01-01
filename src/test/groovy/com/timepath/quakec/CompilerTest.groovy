package com.timepath.quakec

import org.antlr.v4.runtime.ANTLRInputStream
import org.intellij.lang.annotations.Language
import spock.lang.Specification

class CompilerTest extends Specification {
    static def compile(@Language("QuakeC") String it) {
        new Compiler().parse(new ANTLRInputStream(it))
    }

    def "Compiles"() {
        when:
        compile """\
void main() {
    float a = 6;
    float b;
    {
        float c;
        float d = 7;
    }
}
"""
        then:
        notThrown(Throwable)
    }
}
