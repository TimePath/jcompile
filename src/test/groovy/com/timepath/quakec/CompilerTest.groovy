package com.timepath.quakec

import com.timepath.quakec.vm.Program
import org.intellij.lang.annotations.Language
import spock.lang.Specification

class CompilerTest extends Specification {
    static def compile(@Language("QuakeC") String input) {
        new Compiler()
                .include(input)
                .compile()
    }

    static def exec(@Language("QuakeC") String input) {
        new Program(compile(input)).exec()
    }

    def "Compiles"() {
        when:
        compile """void main() {
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

    def "Execute"() {
        when:
        exec """void main() {
    print("Test\\n");
}
"""
        then:
        notThrown(Throwable)
    }
}
