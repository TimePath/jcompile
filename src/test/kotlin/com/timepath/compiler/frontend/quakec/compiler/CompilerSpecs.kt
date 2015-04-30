package com.timepath.quakec.compiler

import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.BlockExpression
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.Generator.ASM
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.compiler.test.PrintVisitor
import com.timepath.q1vm.Program
import junit.framework.TestCase
import junit.framework.TestSuite
import org.junit.runner.RunWith
import org.junit.runners.AllTests
import java.io.File
import kotlin.platform.platformStatic
import kotlin.test.assertEquals

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
        // fail("Nothing to compare")
    }
}

val logger = Logger.new()

/**
 *  RunWith(javaClass<AllTests>())
 *  class XSpecs {
 *      companion object {
 *          platformStatic fun suite() =
 */
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
            val tests = resources.listFiles().sequence().filter {
                !it.isDirectory() && it.name.matches(".+\\.q[ch]$")
            }
            tests.forEach { test ->
                on(test.name) {
                    val compiler = Compiler(QCC(), Q1VM())
                    compiler.include(test)

                    var roots: Sequence<List<Expression>>
                    it("should parse") {
                        logger.info { "Parsing $test" }
                        roots = compiler.ast().toList().sequence()
                        val actual = PrintVisitor.render(BlockExpression(roots.last(), null))
                        compare("AST", test.name + ".xml", actual)
                    }
                    var asm: ASM
                    it("should compile") {
                        logger.info { "Compiling $test" }
                        asm = compiler.compile(roots)
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
                    if (test.name.endsWith(".qc")) {
                        it("should execute") {
                            logger.info { "Executing $test" }
                            Program(asm.generateProgs()).exec()
                        }
                    }
                }
            }
        }
    }
}
