package com.timepath.quakec.compiler

import java.io.File
import kotlin.test.assertEquals
import com.timepath.quakec.Logging
import com.timepath.quakec.compiler.ast.BlockExpression
import com.timepath.quakec.compiler.ast.Expression
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.gen.IR
import com.timepath.quakec.vm.Program
import com.timepath.quakec.vm.ProgramData
import org.intellij.lang.annotations.Language
import org.jetbrains.spek.api.Spek
import com.timepath.quakec.compiler.gen.Generator.ASM
import com.timepath.quakec.QCC

val opts = CompilerOptions()

fun compile([Language("QuakeC")] input: String): ProgramData {
    return Compiler(QCC, opts)
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

val logger = Logging.new()

class CompilerSpecs : Spek() {{
    given("a compiler") {
        val tests = File(resources, "all.src").readLines().map { File(resources, it) }
        .filter { it.exists() }
        tests.forEach {
            on(it.name) {
                val compiler = Compiler(QCC, opts)
                compiler.include(File(resources, "defs.qh"))
                compiler.include(it)

                var roots: List<List<Expression>>?
                it("should parse") {
                    logger.info("Parsing $it")
                    roots = compiler.ast()
                    val actual = BlockExpression(roots!!.last(), null).toStringRecursive()
                    compare("AST", it.name + ".xml", actual)
                }
                var ctx: Generator?
                var asm: ASM?
                it("should compile") {
                    logger.info("Compiling $it")
                    ctx = Generator(compiler.opts)
                    asm = ctx!!.generate(roots!!.flatMap { it })
                    asm!!.ir.map { ir ->
                        if (ir.real)
                            "$ir"
                        else
                            "/* $ir */"
                    }.filterNotNull().joinToString("\n").let { actual ->
                        compare("ASM", it.name + ".asm", actual)
                    }
                    ctx!!.allocator.toString().let { actual ->
                        compare("allocation", it.name + ".txt", actual)
                    }
                }
                it("should execute") {
                    logger.info("Executing $it")
                    Program(asm!!.generateProgs()).exec()
                }
            }
        }
    }
}
}
