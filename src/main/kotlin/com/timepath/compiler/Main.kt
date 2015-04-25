package com.timepath.compiler

import com.timepath.Logger
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.q1vm.util.IOWrapper
import com.timepath.q1vm.util.ProgramDataWriter
import com.timepath.time
import org.anarres.cpp.Feature
import java.io.File
import kotlin.concurrent.thread
import kotlin.platform.platformStatic

object Main {

    val logger = Logger.new()

    platformStatic fun main(args: Array<String>) {
        val xonotic = "${System.getProperties()["user.home"]}/projects/xonotic/xonotic"
        time(logger, "Total time") {
            [data] class Project(val root: String, val define: String, val out: String)

            val defs = listOf(
                    Project("menu", "MENUQC", "menuprogs.dat")
                    , Project("client", "CSQC", "csprogs.dat")
                    , Project("server", "SVQC", "progs.dat")
            )
            defs.forEach { project ->
                time(logger, "Project time") {
                    val compiler = Compiler(QCC(), Q1VM()).let {
                        it.includeFrom(File(xonotic, "data/xonotic-data.pk3dir/qcsrc/${project.root}/progs.src"))
                        it.define(project.define)
                        it
                    }
                    val compiled = compiler.compile()
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
                                    node("file") { +(File(xonotic).relativePath(File(it.file))) }
                                    node("line") { append(it.line) }
                                    node("col") { append(it.col) }
                                    node("reason") { +(it.reason) }
                                    node("extract") { +(it.code) }
                                }
                            }
                        }
                    }.let { File("out", "${project.root}.xml").writeText(it.substring(1)) }
                    thread {
                        ProgramDataWriter(IOWrapper.File(File("out", project.out).let {
                            it.getParentFile().mkdirs()
                            it.createNewFile()
                            it
                        }, write = true)).write(compiled.generateProgs())
                    }
                }
            }
        }
        time(logger, "GMQCC tests") {
            val gmqcc = Compiler(QCC().let {
                it.cpp.addFeatures(Feature.DIGRAPHS, Feature.TRIGRAPHS)
                it
            }, Q1VM()).let {
                it.define("GMQCC")
                it.define("__STD_GMQCC__")
                it
            }
            val include = { filter: (file: File) -> Boolean ->
                val files = File("$xonotic/gmqcc/tests").listFiles(filter)
                if (files != null) {
                    files.sort()
                    files.forEach {
                        gmqcc.include(it)
                    }
                }
            }
            include { it.name.endsWith(".qh") }
            // include { it.name.endsWith(".qc") }
            gmqcc.include(File("$xonotic/gmqcc/tests/fieldparams.qc"))
            gmqcc.compile()
        }
    }
}
