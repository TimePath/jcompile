package com.timepath.compiler.backends.cpp

import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.ASTVisitor
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backends.q1vm.gen.type
import com.timepath.compiler.frontend.quakec.QCC
import org.stringtemplate.v4.Interpreter
import org.stringtemplate.v4.ST
import org.stringtemplate.v4.STGroup
import org.stringtemplate.v4.STGroupFile
import org.stringtemplate.v4.misc.ObjectModelAdaptor
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class CPPPrinter(val templ: STGroup) : ASTVisitor<String> {
    override fun default(e: Expression): String {
        val s = e.simpleName
        val ST = templ.getInstanceOf(s)
        if (ST == null) {
            throw NullPointerException("Missing template: $s")
        }
        return ST
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

fun project(project: Project) {
    val sourceRoot = File("$xonotic/data/xonotic-data.pk3dir/qcsrc/${project.root}")
    val compiler = Compiler(QCC())
            .includeFrom(File(sourceRoot, "progs.src"))
            .define(project.define)

    val templates = STGroupFile(javaClass<CPPPrinter>().getResource("/com/timepath/compiler/backends/cpp/cpp.stg"), "UTF-8", '<', '>')
    val printer = CPPPrinter(templates)
    templates.registerModelAdaptor(javaClass<Expression>(), object : ObjectModelAdaptor() {
        override fun getProperty(interpreter: Interpreter?, self: ST?, o: Any?, property: Any?, propertyName: String?): Any? {
            val e = o as Expression
            return when (propertyName) {
                "type" -> e.type(compiler.state)
                else -> super.getProperty(interpreter, self, o, property, propertyName)
            }
        }
    })
    templates.registerRenderer(javaClass<Expression>(), { it, format, locale ->
        (it as Expression).accept(printer)
    })

    val ast = compiler.ast()
    val projOut = File(out, project.out)
    projOut.mkdirs()
    val predef = File(projOut, "progs.h")
    FileOutputStream(predef).writer().buffered().use {
        val predefs = javaClass<CPPPrinter>().getResourceAsStream("/com/timepath/compiler/backends/cpp/predefs.hpp")
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
        module.add("files", listOf(predef.name) + map.keySet())
        it.write(module.render())
    }
    fun write(file: File, include: List<File>, code: List<Expression>) {
        val parent = file.getParentFile()
        parent.mkdirs()
        FileOutputStream(file).writer().buffered().use {
            val st = templates.getInstanceOf("File")!!
            st.add("includes", include.map {
                parent.toPath().relativize(it.toPath())
            })
            st.add("namespace", ns)
            st.add("code", code)
            it.write(st.render())
        }
    }

    val include = listOf(predef)
    val accumulate = linkedListOf<Expression>()
    for ((f, code) in map) {
        write(File(projOut, f), include, code)
        accumulate.addAll(code)
    }
    write(File(projOut, "all.cpp"), include, accumulate)
}

inline fun time(name: String, action: () -> Unit) {
    val start = Date()
    action()
    logger.info("$name: ${(Date().getTime() - start.getTime()).toDouble() / 1000} seconds")
}

fun main(args: Array<String>) {
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
                project(project)
            }
        }
    }
}
