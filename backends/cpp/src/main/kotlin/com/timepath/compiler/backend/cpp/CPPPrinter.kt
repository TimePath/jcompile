package com.timepath.compiler.backend.cpp

import java.io.File
import java.io.FileOutputStream
import java.util.Date
import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.*
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.compiler.types.*
import org.stringtemplate.v4.*

class CPPPrinter(val templ: STGroup) : ASTVisitor<String> {
    override fun default(e: Expression): String {
        return templ.getInstanceOf(e.javaClass.getSimpleName())
                .add("e", e)
                .render()
    }
}


val logger = Logger.new()

val xonotic = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic"

[data] class Project(val root: String, val define: String, val out: String)

val subprojects = listOf(
        Project("menu", "MENUQC", "menuprogs.c")
        , Project("client", "CSQC", "csprogs.c")
        , Project("server", "SVQC", "progs.c")
)
val out = File("out")
val ns = "xon"

inline fun time(name: String, action: () -> Unit) {
    val start = Date()
    action()
    logger.info("$name: ${(Date().getTime() - start.getTime()).toDouble() / 1000} seconds")
}

fun main(args: Array<String>) {
    val templates = STGroupFile(javaClass<CPPPrinter>().getResource("/com/timepath/compiler/backend/cpp/cpp.stg"), "UTF-8", '<', '>')
    val printer = CPPPrinter(templates)
    templates.registerRenderer(javaClass<Expression>(), {(it, format, locale) ->
        (it as Expression).accept(printer)
    })
    templates.registerRenderer(javaClass<Type>(), {(it, format, locale) ->
        templates.getInstanceOf("type_" + it.javaClass.getSimpleName())
                .add("it", it)
                .add("id", format)
                .render()
    })
    out.mkdirs()
    time("Total time") {
        FileOutputStream(File(out, "CMakeLists.txt")).writer().use {
            it.write("""cmake_minimum_required(VERSION 2.8)
project(Test)
${subprojects.map { "add_subdirectory(${it.out})" }.join("\n")}
""")
        }
        for (project in subprojects) {
            time("Project time") {
                val sourceRoot = File("$xonotic/data/xonotic-data.pk3dir/qcsrc/${project.root}")
                val compiler = Compiler(QCC)
                        .includeFrom(File(sourceRoot, "progs.src"))
                        .define(project.define)

                val ast = compiler.ast()
                val projOut = File(out, project.out)
                projOut.mkdirs()
                val predef = File(projOut, "progs.h")
                FileOutputStream(predef).writer().buffered().use {
                    val predefs = javaClass<CPPPrinter>().getResourceAsStream("/com/timepath/compiler/backend/cpp/predefs.hpp")
                    it.write("")
                    it.appendln("namespace $ns {")
                    predefs.reader().buffered().copyTo(it)
                    it.appendln("}")
                }
                val zipped = compiler.includes.map {
                    sourceRoot.getParentFile().relativePath(File(it.path))
                            .replace(".qc", ".cpp")
                            .replace(".qh", ".hpp")
                }.zip(ast)
                val map = zipped.filter { !it.first.contains("<") }.toMap()
                FileOutputStream(File(projOut, "CMakeLists.txt")).writer().buffered().use {
                    val module = templates.getInstanceOf("Module")!!
                    module.add("root", project.root)
                    module.add("files", listOf(predef) + map.keySet())
                    it.write(module.render())
                }
                val include = linkedListOf(predef)
                val accumulate = linkedListOf<Expression>()
                for ((f, code) in map) {
                    accumulate.addAll(code)
                    val file = File(projOut, f)
                    val parent = file.getParentFile()
                    parent.mkdirs()
                    val header = /* f.endsWith(".h") */ true
                    FileOutputStream(file).writer().buffered().use {
                        val st = templates.getInstanceOf("File")!!
                        st.add("includes", include.map {
                            parent.toPath().relativize(it.toPath())
                        })
                        st.add("namespace", ns)
                        st.add("code", code)
                        it.write(st.render())
                    }
                    if (header)
                        include.add(File(projOut, f))
                }
            }
        }
    }
}
