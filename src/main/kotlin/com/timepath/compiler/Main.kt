package com.timepath.compiler

import com.timepath.Logger
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.q1vm.util.IOWrapper
import com.timepath.q1vm.util.ProgramDataWriter
import com.timepath.with
import java.io.File
import kotlin.concurrent.thread
import kotlin.platform.platformStatic

object Main {

    val logger = Logger()

    platformStatic fun main(args: Array<String>) {
        require(args.size() == 1, "qcsrc path required")
        val root = File(args[0])
        require(root.exists(), "qcsrc not found")
        time(logger, "Total time") {
            @data class Project(val root: String, val define: String, val out: String)

            val defs = listOf(
                    Project("menu", "MENUQC", "menuprogs.dat")
                    , Project("client", "CSQC", "csprogs.dat")
                    , Project("server", "SVQC", "progs.dat")
            )
            for (project in defs) {
                time(logger, "Project time") {
                    val compiler = Compiler(QCC(), Q1VM()) with {
                        includeFrom(File(root, "${project.root}/progs.src"))
                        define(project.define)
                    }
                    val compiled = compiler.compile()
                    thread {
                        fun StringBuilder.node(s: String, body: StringBuilder.() -> Unit) {
                            append("\n<$s>")
                            body()
                            append("</$s>")
                        }
                        StringBuilder {
                            fun Any.plus() = append(this.toString()
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
                        }.let { File("out", "${project.root}.xml").writeText(it.substring(1)) }
                    }
                    thread {
                        ProgramDataWriter(IOWrapper.File(File("out", project.out) with {
                            getParentFile().mkdirs()
                            createNewFile()
                        }, write = true)).write(compiled.generateProgs())
                    }
                }
            }
        }
    }
}
