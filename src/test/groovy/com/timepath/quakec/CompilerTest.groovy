package com.timepath.quakec

import org.antlr.v4.runtime.ANTLRInputStream
import spock.lang.Specification

class CompilerTest extends Specification {
    def "Compiles"() {
        setup:
        def compile = { String it -> new Compiler().parse(new ANTLRInputStream(it)) }

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
