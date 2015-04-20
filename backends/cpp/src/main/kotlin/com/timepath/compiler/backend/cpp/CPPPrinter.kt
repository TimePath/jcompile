package com.timepath.compiler.backend.cpp

import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.ASTVisitor
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.type
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.time
import org.stringtemplate.v4.Interpreter
import org.stringtemplate.v4.ST
import org.stringtemplate.v4.STGroup
import org.stringtemplate.v4.STGroupFile
import org.stringtemplate.v4.misc.ObjectModelAdaptor
import java.io.File
import java.io.FileOutputStream
import kotlin.platform.platformStatic

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

    companion object {

        val logger = Logger.new()

        data class Project(val root: String, val define: String, val out: String)

        val subprojects = listOf(
                Project("menu", "MENUQC", "menuprogs.c")
                , Project("client", "CSQC", "csprogs.c")
                , Project("server", "SVQC", "progs.c")
        )
        val out = File("out")
        val ns = "xon"

        val STG = javaClass<CPPPrinter>().getResource("/com/timepath/compiler/backend/cpp/cpp.stg")
        val PREDEFS = javaClass<CPPPrinter>().getResource("/com/timepath/compiler/backend/cpp/predefs.hpp")

        fun project(root: File, project: Project) {
            val sourceRoot = File(root, project.root)
            val compiler = Compiler(QCC(), Q1VM()).let {
                it.includeFrom(File(sourceRoot, "progs.src"))
                it.define(project.define)
                it
            }

            val templates = STGroupFile(STG, "UTF-8", '<', '>').let {
                val printer = CPPPrinter(it)
                it.registerModelAdaptor(javaClass<Expression>(), object : ObjectModelAdaptor() {
                    override fun getProperty(interpreter: Interpreter?, self: ST?, o: Any?, property: Any?, propertyName: String?): Any? {
                        o as Expression
                        return when (propertyName) {
                            "type" -> o.type(compiler.state)
                            else -> super.getProperty(interpreter, self, o, property, propertyName)
                        }
                    }
                })
                it.registerRenderer(javaClass<Expression>(), { it, format, locale ->
                    (it as Expression).accept(printer)
                })
                it
            }

            val ast = compiler.ast()
            val projOut = File(out, project.out)
            projOut.mkdirs()
            val predef = File(projOut, "progs.h")
            FileOutputStream(predef).writer().buffered().use {
                it.write("")
                it.appendln("namespace $ns {")
                PREDEFS.openStream().reader().buffered().copyTo(it)
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

        platformStatic fun main(args: Array<String>) {
            require(args.size() == 1, "qcsrc path required")
            val root = File(args[0])
            require(root.exists(), "qcsrc not found")
            out.mkdirs()
            time(logger, "Total time") {
                FileOutputStream(File(out, "CMakeLists.txt")).writer().use {
                    it.write("""
cmake_minimum_required(VERSION 2.8)
project(Test)

${subprojects.map { "add_subdirectory(${it.out})" }.join("\n")}
""".substring(1))
                }
                for (project in subprojects) {
                    time(logger, "Project time") {
                        project(root, project)
                    }
                }
            }
        }
    }
}
