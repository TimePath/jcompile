package com.timepath.compiler.frontend.quakec

import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.BlockExpression
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.ir.ASMPrinter
import com.timepath.q1vm.Program
import com.timepath.q1vm.util.ProgramDataWriter
import junit.framework.TestCase
import junit.framework.TestSuite
import org.junit.runner.RunWith
import org.junit.runners.AllTests
import java.io.File
import kotlin.test.assertEquals

val resources = File("src/test/resources")

fun testTemp(test: File, ext: String) = File(resources, "tmp/${test.nameWithoutExtension}/${test.nameWithoutExtension}.$ext")

fun compare(what: String, test: File, ext: String, actual: String) {
    val saved = File(resources, "${test.nameWithoutExtension}.$ext")
    if (saved.exists()) {
        val expected = saved.readText()
        assertEquals(expected, actual, "$what differs")
    } else {
        val temp = testTemp(test, ext)
        temp.parentFile.mkdirs()
        temp.writeText(actual)
        // fail("Nothing to compare")
    }
}

val logger = Logger()

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

@RunWith(AllTests::class)
class CompilerSpecs {
    companion object {
        @JvmStatic fun suite() = given("a compiler") {
            val tests = resources.listFiles().filter {
                !it.isDirectory && it.name.matches(".+\\.q[ch]$".toRegex())
            }.sortedBy { it.name }
            tests.forEach { test ->
                on(test.name) {
                    val compiler = Compiler(QCC(), Q1VM())
                    compiler.include(test)

                    var roots: List<List<Expression>>? = null
                    it("should parse") {
                        logger.info { "Parsing $test" }
                        roots = compiler.parse().toList()
                        val actual = PrintVisitor.render(BlockExpression(roots!!.last(), null))
                        compare("AST", test, "xml", actual)
                    }
                    var prog: Program? = null
                    it("should compile") {
                        logger.info { "Compiling $test" }
                        val asm = compiler.compile(roots!!.asSequence())
                        ASMPrinter(asm.ir).toString().let { actual ->
                            compare("ASM", test, "asm", actual)
                        }
                        compiler.state.allocator.toString().let { actual ->
                            compare("allocation", test, "txt", actual)
                        }
                        val progData = asm.generateProgs()
                        prog = Program(progData)
                        ProgramDataWriter(testTemp(test, "dat")).write(progData)
                    }
                    if (test.name.endsWith(".qc")) {
                        it("should execute") {
                            logger.info { "Executing $test" }
                            prog!!.exec()
                        }
                    }
                }
            }
        }
    }
}
