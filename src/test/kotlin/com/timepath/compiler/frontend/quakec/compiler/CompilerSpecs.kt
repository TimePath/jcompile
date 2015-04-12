package com.timepath.quakec.compiler

import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.CompilerOptions
import com.timepath.compiler.PrintVisitor
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.ast.BlockExpression
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.compiler.gen.Generator.ASM
import com.timepath.q1vm.Program
import com.timepath.q1vm.ProgramData
import junit.framework.TestCase
import junit.framework.TestSuite
import org.intellij.lang.annotations.Language
import org.junit.runner.RunWith
import org.junit.runners.AllTests
import java.io.File
import kotlin.platform.platformStatic
import kotlin.test.assertEquals

val opts = CompilerOptions()

fun compile([Language("QuakeC")] input: String): ProgramData {
    return Compiler(QCC, CompileState(opts))
            .include(input, "-")
            .compile() as ProgramData
}

fun exec([Language("QuakeC")] input: String) {
    Program(compile(input)).exec("main")
}

val resources = File("src/test/resources")

fun compare(what: String, name: String, actual: String) {
    val saved = File(resources, name)
    if (saved.exists()) {
        val expected = saved.readText()
        assertEquals(expected, actual, "$what differs")
    } else {
        val temp = File(resources, "tmp/" + name)
        temp.getParentFile().mkdirs()
        temp.writeText(actual)
        //        fail("Nothing to compare")
    }
}

val logger = Logger.new()

inline fun given(given: String, on: TestSuite.() -> Unit) = TestSuite("given $given").let { it.on(); it }
inline fun TestSuite.on(what: String, assertions: ((String, () -> Unit) -> Unit) -> Unit) = TestSuite("$what.it").let {
    assertions { assertion, run ->
        it.addTest(object : TestCase("$assertion ($what)") {
            override fun runTest() = run()
        })
    }
    addTest(it)
}

RunWith(javaClass<AllTests>())
class CompilerSpecs {
    companion object {
        platformStatic fun suite() = given("a compiler") {
            val tests = File(resources, "all.src").readLines().sequence()
                    .map { File(resources, it) }
                    .filter { it.exists() }
            tests.forEach { test ->
                on(test.name) {
                    val compiler = Compiler(QCC, CompileState(opts))
                    compiler.include(test)

                    var roots: List<List<Expression>>
                    it("should parse") {
                        logger.info("Parsing $test")
                        roots = compiler.ast()
                        val actual = PrintVisitor.render(BlockExpression(roots.last(), null))
                        compare("AST", test.name + ".xml", actual)
                    }
                    var asm: ASM
                    it("should compile") {
                        logger.info("Compiling $test")
                        asm = compiler.state.gen.generate(roots.flatMap { it })
                        asm.ir.map { ir ->
                            if (ir.real)
                                "$ir"
                            else
                                "/* $ir */"
                        }.filterNotNull().joinToString("\n").let { actual ->
                            compare("ASM", test.name + ".asm", actual)
                        }
                        compiler.state.allocator.toString().let { actual ->
                            compare("allocation", test.name + ".txt", actual)
                        }
                    }
                    it("should execute") {
                        logger.info("Executing $test")
                        Program(asm.generateProgs()).exec()
                    }
                }
            }
        }
    }
}
