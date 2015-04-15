package com.timepath.compiler

import com.timepath.Logger
import com.timepath.compiler.backends.q1vm.CompilerOptions
import com.timepath.compiler.backends.q1vm.Q1VM
import com.timepath.compiler.backends.q1vm.gen.Generator
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.q1vm.util.IOWrapper
import com.timepath.q1vm.util.ProgramDataWriter
import org.anarres.cpp.Feature
import java.io.File

val logger = Logger.new()

fun time(name: String, action: () -> Unit) {
    val start = System.currentTimeMillis()
    action()
    val end = System.currentTimeMillis()
    logger.info("$name: ${(end - start).toDouble() / 1000} seconds")
}

fun main(args: Array<String>) {
    val opts = CompilerOptions()
    val xonotic = "${System.getProperties()["user.home"]}/projects/xonotic/xonotic"
    time("Total time") {
        [data] class Project(val root: String, val define: String, val out: String)

        val defs = listOf(
                Project("menu", "MENUQC", "menuprogs.dat"),
                Project("client", "CSQC", "csprogs.dat"),
                Project("server", "SVQC", "progs.dat")
        )
        defs.forEach { project ->
            time("Project time") {
                val compiler = Compiler(QCC(), Q1VM())
                        .includeFrom(File("$xonotic/data/xonotic-data.pk3dir/qcsrc/${project.root}/progs.src"))
                        .define(project.define)
                val compiled = compiler.compile()
                ProgramDataWriter(IOWrapper.File(File("out", project.out).let {
                    it.getParentFile().mkdirs()
                    it.createNewFile()
                    it
                }, write = true)).write((compiled as Generator.ASM).generateProgs())
            }
        }
    }
    time("GMQCC tests") {
        val gmqcc = Compiler(QCC().let {
            it.preprocessor.addFeatures(Feature.DIGRAPHS, Feature.TRIGRAPHS)
            it
        }, Q1VM())
                .define("GMQCC")
                .define("__STD_GMQCC__")
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
        //        include { it.name.endsWith(".qc") }
        gmqcc.include(File("$xonotic/gmqcc/tests/fieldparams.qc"))
        gmqcc.compile()
    }
}
