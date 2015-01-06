package com.timepath.quakec

import org.jetbrains.spek.api.Spek
import com.timepath.quakec.vm.Program
import org.intellij.lang.annotations.Language
import com.timepath.quakec.vm.ProgramData

fun compile([Language("QuakeC")] input: String): ProgramData {
    return Compiler()
            .include(input, "-")
            .compile()
}

fun exec([Language("QuakeC")] input: String) {
    Program(compile(input)).exec("main")
}

class CompilerSpecs : Spek() {{
    given("A compiler") {
        on("code") {
            val code = """
void main() {
    float a = 6;
    float b;
    {
        float c;
        float d = 7;
    }
}
"""
            it("should compile") {
                compile(code)
            }
        }
        on("exec") {
            val code = """
void main() {
    print("Test\\n");
}
"""
            it("should execute") {
//                exec(code) // TODO
            }
        }
    }
}
}