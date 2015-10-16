package com.timepath.compiler.backend.lua

import com.timepath.Logger
import com.timepath.Printer
import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.backend.q1vm.types.field_t
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.compiler.time
import java.io.File
import java.io.FileOutputStream

object LuaPrinter {

    val logger = Logger()

    data class Project(val root: String, val define: String, val out: String)

    val subprojects = listOf(
            Project("menu", "MENUQC", "menuprogs.lua")
            , Project("client", "CSQC", "csprogs.lua")
            , Project("server", "SVQC", "progs.lua")
    )
    val out = File("out")

    val indent = "    "

    fun write(visitor: PrintVisitor, file: File, code: List<Expression>) {
        val parent = file.getParentFile()
        parent.mkdirs()
        FileOutputStream(file).writer().buffered().use {
            it.write(Printer {
                code.asSequence().filter {
                    when {
                    // Pointer to member
                        it is DeclarationExpression
                                && it.type is field_t
                                && it.value != null
                        -> false
                        else -> true
                    }
                }.forEach { +it.accept(visitor) }
                +""
            }.toString())
        }
    }

    fun project(root: File, project: Project) {
        val sourceRoot = File(root, project.root)
        val compiler = Compiler(QCC(), Q1VM()).let {
            it.includeFrom(File(sourceRoot, "progs.src"))
            it.define(project.define)
            it
        }
        val v = PrintVisitor(indent)
        val ast = compiler.parse().toList()
        val projOut = File(out, project.out)
        projOut.mkdirs()
        write(v, File(projOut, "all.lua"), ast.flatMap { it })
    }

    @JvmStatic fun main(args: Array<String>) {
        require(args.size() == 1) { "qcsrc path required" }
        val root = File(args[0])
        require(root.exists()) { "qcsrc not found" }
        out.mkdirs()
        time(logger, "Total time") {
            for (project in subprojects) {
                time(logger, "Project time") {
                    project(root, project)
                }
            }
        }
    }
}
