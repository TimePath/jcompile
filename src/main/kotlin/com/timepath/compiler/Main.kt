package com.timepath.compiler

import com.timepath.Logger
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.compiler.ir.ASMPrinter
import com.timepath.q1vm.util.IOWrapper
import com.timepath.q1vm.util.ProgramDataWriter
import java.io.File
import kotlin.concurrent.thread

object Main {

    val logger = Logger()

    @JvmStatic fun main(args: Array<String>) {
        require(args.size == 1) { "qcsrc path required" }
        val root = File(args[0])
        require(root.exists()) { "qcsrc not found" }
        time(logger, "Total time") {
            data class Project(val root: String, val define: String, val out: String)

            val defs = listOf(
                    Project("menu", "MENUQC", "menuprogs.dat")
                    // , Project("client", "CSQC", "csprogs.dat")
                    // , Project("server", "SVQC", "progs.dat")
            )
            for (project in defs) {
                time(logger, "Project time") {
                    val compiler = Compiler(QCC(), Q1VM()).apply {
                        include(File(root, "../.tmp/${project.root}.qc"))
                        define(project.define)
                    }
                    val compiled = compiler.compile()
                    val out = File("out")
                    out.mkdir()
                    thread {
                        File(out, "${project.root}.asm").writeText(ASMPrinter(compiled.ir).toString())
                    }
                    thread {
                        fun StringBuilder.node(s: String, body: StringBuilder.() -> Unit) {
                            append("\n<$s>")
                            body()
                            append("</$s>")
                        }
                        StringBuilder().apply {
                            operator fun Any.unaryPlus() = append(this.toString()
                                    .replace("&", "&amp;")
                                    .replace("<", "&lt;"))
                            node("errors") {
                                compiler.state.errors.forEach {
                                    node("error") {
                                        node("file") { +(File(it.file).relativeTo(root)) }
                                        node("line") { append(it.line) }
                                        node("col") { append(it.col) }
                                        node("reason") { +(it.reason) }
                                        node("extract") { +(it.code) }
                                    }
                                }
                            }
                        }.let {
                            File(out, "${project.root}.xml").writeText(it.substring(1))
                        }
                    }
                    thread {
                        ProgramDataWriter(IOWrapper.File(File(out, project.out).apply { createNewFile() }, write = true))
                                .write(compiled.generateProgs())
                        File(out, "${project.root}.txt").writeText(compiler.state.allocator.toString())
                    }
                }
            }
        }
    }
}
