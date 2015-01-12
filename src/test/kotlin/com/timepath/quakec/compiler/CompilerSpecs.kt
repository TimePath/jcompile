package com.timepath.quakec.compiler

import java.io.File
import kotlin.test.assertEquals
import com.timepath.quakec.Logging
import com.timepath.quakec.compiler.ast.BlockStatement
import com.timepath.quakec.compiler.ast.Statement
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.vm.Program
import com.timepath.quakec.vm.ProgramData
import org.intellij.lang.annotations.Language
import org.jetbrains.spek.api.Spek

fun compile([Language("QuakeC")] input: String): ProgramData {
    return Compiler()
            .include(input, "-")
            .compile()
}

fun exec([Language("QuakeC")] input: String) {
    Program(compile(input)).exec("main")
}

val logger = Logging.new()

class CompilerSpecs : Spek() {{
    given("a compiler") {
        val resources = File("src/test/resources")
        val tests = resources.listFiles {
            it.name.matches(".+qc")
        }!!.toSortedList()
        tests.forEach {
            on(it.name) {
                val compiler = Compiler()
                compiler.include(File(resources, "defs.qh"))
                compiler.include(it)

                var roots: List<List<Statement>>?
                it("should parse") {
                    logger.info("Parsing $it")
                    roots = compiler.ast()
                    val actual = BlockStatement(roots!!.last()).toStringRecursive()
                    val saved = File(resources, "${it.name}.xml")
                    if (saved.exists()) {
                        val expected = saved.readText()
                        assertEquals(expected, actual, "AST differs")
                    } else {
                        val temp = File(resources, "${it.name}.xml.tmp")
                        temp.getParentFile().mkdirs()
                        temp.writeText(actual)
//                        fail("Nothing to compare")
                    }
                }
                var ctx: Generator?
                var asm: List<IR>?
                it("should compile") {
                    logger.info("Compiling $it")
                    ctx = Generator(roots!!.flatMap { it })
                    asm = ctx!!.generate()
                    val actual = asm!!.map { ir ->
                        if (ir.real)
                            ir.toString()
                        else
                            null
                    }.filterNotNull().joinToString("\n")
                    val saved = File(resources, "${it.name}.asm")
                    if (saved.exists()) {
                        val expected = saved.readText()
                        assertEquals(expected, actual, "ASM differs")
                    } else {
                        val temp = File(resources, "${it.name}.asm.tmp")
                        temp.getParentFile().mkdirs()
                        temp.writeText(actual)
//                        fail("Nothing to compare")
                    }
                }
                it("should execute") {
                    logger.info("Executing $it")
                    Program(ctx!!.generateProgs(asm!!)).exec()
                }
            }
        }
    }
}
}