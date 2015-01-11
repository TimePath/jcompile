package com.timepath.quakec.compiler

import org.jetbrains.spek.api.Spek
import com.timepath.quakec.vm.Program
import org.intellij.lang.annotations.Language
import com.timepath.quakec.vm.ProgramData
import java.io.File
import com.timepath.quakec.compiler.ast.BlockStatement
import com.timepath.quakec.compiler.gen.GenerationContext
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.test.fails

fun compile([Language("QuakeC")] input: String): ProgramData {
    return Compiler()
            .include(input, "-")
            .compile()
}

fun exec([Language("QuakeC")] input: String) {
    Program(compile(input)).exec("main")
}

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
                val roots = compiler.ast()
                it("should parse") {
                    val actual = BlockStatement(roots.last()).toStringRecursive()
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
                it("should compile") {
                    val ctx = GenerationContext(roots.flatMap { it })
                    val asm = ctx.generate()
                    val actual = asm.map { ir ->
                        if (!ir.dummy)
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
                    Program(compiler.compile(roots)).exec()
                }
            }
        }
    }
}
}