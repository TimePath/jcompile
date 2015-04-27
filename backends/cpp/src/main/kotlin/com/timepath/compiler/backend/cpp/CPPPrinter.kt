package com.timepath.compiler.backend.cpp

import com.timepath.Logger
import com.timepath.compiler.Compiler
import com.timepath.compiler.ast.BlockExpression
import com.timepath.compiler.ast.Expression
import com.timepath.compiler.backend.q1vm.Q1VM
import com.timepath.compiler.frontend.quakec.QCC
import com.timepath.time
import java.io.File
import java.io.FileOutputStream
import kotlin.platform.platformStatic

class CPPPrinter {

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

        val PREDEFS = javaClass<CPPPrinter>().getResource("/com/timepath/compiler/backend/cpp/predefs.hpp")

        val indent = "    "

        fun write(visitor: PrintVisitor, file: File, include: List<File>, code: List<Expression>) {
            val parent = file.getParentFile()
            parent.mkdirs()
            FileOutputStream(file).writer().buffered().use {
                it.write(Printer {
                    +"#pragma once"
                    +""
                    include.forEach {
                        +"#include \"${parent.toPath().relativize(it.toPath())}\""
                    }
                    +""
                    +"namespace $ns"
                    +BlockExpression(code).accept(visitor)
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

            val ast = compiler.ast().toList()
            val projOut = File(out, project.out)
            projOut.mkdirs()
            val predef = File(projOut, "progs.h")
            FileOutputStream(predef).writer().buffered().use {
                it.write(Printer {
                    +"#pragma once"
                    +"namespace $ns {"
                    +indent {
                        PREDEFS.openStream().reader().buffered().lines().forEach {
                            +it
                        }
                    }
                    +"}"
                }.toString())
            }
            val zipped = compiler.includes.map {
                sourceRoot.getParentFile().relativePath(File(it.path))
                        .replace(".qc", ".cpp")
                        .replace(".qh", ".hpp")
            }.zip(ast)
            val map = zipped.filter { !it.first.contains("<") }.toMap()
            FileOutputStream(File(projOut, "CMakeLists.txt")).writer().buffered().use {
                it.write(Printer {
                    +"cmake_minimum_required(VERSION 2.8)"
                    +"project(${project.root})"
                    +"add_executable(${project.root}"
                    +indent {
                        +predef.name
                        map.keySet().forEach {
                            +it
                        }
                    }
                    +")"
                    +"# target_compile_features(${project.root} PRIVATE cxx_explicit_conversions)"
                    +"add_definitions(-std=c++11)"
                }.toString())
            }
            val v = PrintVisitor(compiler.state, indent)
            val include = listOf(predef)
            val accumulate = linkedListOf<Expression>()
            for ((f, code) in map) {
                write(v, File(projOut, f), include, code)
                accumulate.addAll(code)
            }
            write(v, File(projOut, "all.cpp"), include, accumulate)
        }

        platformStatic fun main(args: Array<String>) {
            require(args.size() == 1, "qcsrc path required")
            val root = File(args[0])
            require(root.exists(), "qcsrc not found")
            out.mkdirs()
            time(logger, "Total time") {
                FileOutputStream(File(out, "CMakeLists.txt")).writer().use {
                    it.write(Printer {
                        +"cmake_minimum_required(VERSION 2.8)"
                        +"project(Test)"
                        +""
                        subprojects.forEach {
                            +"add_subdirectory(${it.out})"
                        }
                    }.toString())
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
