package com.timepath.compiler

import java.io.File
import com.timepath.Logger
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.compiler.gen.Generator
import com.timepath.q1vm.util.ProgramDataWriter
import com.timepath.q1vm.util.IOWrapper
import org.anarres.cpp.Feature

val logger = Logger.new()

fun time(name: String, action: () -> Unit) {
    val start = System.currentTimeMillis()
    action()
    val end = System.currentTimeMillis()
    logger.info("$name: ${(end - start).toDouble() / 1000} seconds")
}

fun main(args: Array<String>) {
    val opts = CompilerOptions()
    val xonotic = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic"
    time("Total time") {
        [data] class Project(val root: String, val define: String, val out: String)

        val defs = listOf(
                Project("menu", "MENUQC", "menuprogs.dat"),
                Project("client", "CSQC", "csprogs.dat"),
                Project("server", "SVQC", "progs.dat")
        )
        defs.forEach { project ->
            time("Project time") {
                val compiler = Compiler(QCC, opts)
                        .includeFrom(File("$xonotic/data/xonotic-data.pk3dir/qcsrc/${project.root}/progs.src"))
                        .define(project.define)
                val compiled = compiler.compile()
                ProgramDataWriter(IOWrapper.File(File("out", project.out), write = true))
                        .write((compiled as Generator.ASM).generateProgs())
            }
        }
    }
    time("GMQCC tests") {
        val gmqcc = Compiler(QCC, opts)
                .define("GMQCC")
                .define("__STD_GMQCC__")
        gmqcc.preprocessor.addFeatures(
                Feature.DIGRAPHS,
                Feature.TRIGRAPHS
        )
        val include = {(filter: (file: File) -> Boolean) ->
            val files = File("$xonotic/gmqcc/tests").listFiles(filter)
            if (files != null) {
                files.sort()
                files.forEach {
                    gmqcc.include(it)
                }
            }
        }
        include { it.name.endsWith(".qh") }
        //        include { it.name.endsWith(".qc") }
        gmqcc.include(File("$xonotic/gmqcc/tests/fieldparams.qc"))
        gmqcc.compile()
    }
}
