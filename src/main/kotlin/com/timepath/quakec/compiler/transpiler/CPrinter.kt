/**
 * Transpiles QuakeC to regular C
 */
package com.timepath.quakec.compiler.transpiler

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import com.timepath.quakec.Logging
import com.timepath.quakec.compiler.CompilerOptions
import com.timepath.quakec.compiler.Type
import com.timepath.quakec.compiler.ast.BinaryExpression
import com.timepath.quakec.compiler.ast.BlockExpression
import com.timepath.quakec.compiler.ast.BreakStatement
import com.timepath.quakec.compiler.ast.ConditionalExpression
import com.timepath.quakec.compiler.ast.ConstantExpression
import com.timepath.quakec.compiler.ast.ContinueStatement
import com.timepath.quakec.compiler.ast.DeclarationExpression
import com.timepath.quakec.compiler.ast.Expression
import com.timepath.quakec.compiler.ast.FunctionExpression
import com.timepath.quakec.compiler.ast.GotoExpression
import com.timepath.quakec.compiler.ast.IndexExpression
import com.timepath.quakec.compiler.ast.LabelExpression
import com.timepath.quakec.compiler.ast.LoopExpression
import com.timepath.quakec.compiler.ast.MemberExpression
import com.timepath.quakec.compiler.ast.MethodCallExpression
import com.timepath.quakec.compiler.ast.Nop
import com.timepath.quakec.compiler.ast.ParameterExpression
import com.timepath.quakec.compiler.ast.ReferenceExpression
import com.timepath.quakec.compiler.ast.ReturnStatement
import com.timepath.quakec.compiler.ast.UnaryExpression
import com.timepath.quakec.compiler.gen.Generator
import org.antlr.v4.runtime.misc.Utils

class CPrinter(val gen: Generator, val all: List<Expression>, val ns: String) {

    var depth: Int = 0

    fun pprint(out: BufferedWriter) {
        gen.generate(all) // FIXME: separate type propagation phase
        for (it in all) {
            it.transform { it.reduce() }
            out.appendln(it.pprint())
        }
    }

    fun term(): String = if (depth == 0) ";" else ""

    fun List<Expression>.pprint(append: String = "", join: String = "\n", action: (it: Expression) -> String = { it.pprint() }): String {
        return map { action(it) + append }.joinToString(join)
    }

    fun Type.pprint(id: kotlin.String? = null, indirection: Int = 0): String {
        val cname = this.toString()
        val stars = "*".repeat(indirection)
        return when (this) {
            is Type.Array -> {
                val expression = when (this.sizeExpr) {
                    is ConstantExpression -> (this.sizeExpr.value.value as Float).toInt().toString()
                    else -> this.sizeExpr.toString()
                }
                "${type.pprint()} $id[$expression]"
            }
            is Type.Field -> "${type.pprint(id, indirection + 1)}"
            is Type.Function -> "${type.pprint()}($stars*$id)(${argTypes.map {
                it.pprint()
            }.joinToString(", ")})"
            else -> {
                when (id) {
                    null -> "$cname$stars"
                    else -> "$cname $stars$id"
                }
            }
        }
    }

    fun pprintConst(v: Any?): String = when (v) {
        null -> "NULL"
        is Array<*> -> "(vector) { ${v.map { pprintConst(it) }.join(", ")} }"
        is String -> '"' + Utils.escapeWhitespace(v, false)/*.replace("\"", "\\\"")*/ + '"'
        is Float -> {
            val i = v.toInt()
            val int = v == i.toFloat()
            when {
                int -> i.toString()
                else -> v.toString() + "f"
            }
        }
        else -> "$v"
    }

    fun Expression.pprint(term: String = ""): String {
        return /*"/* ${this.javaClass.getSimpleName()} */" +*/ when (this) {
            is ConditionalExpression -> {
                if (expression)
                    "(${test.pprint()} ? ${pass.pprint()} : ${fail!!.pprint()})"
                else
                    "if (${test.pprint()})\n${pass.pprint(";")}" + if (fail == null) "" else "\nelse\n${fail.pprint(";")}"
            }
            is ConstantExpression -> {
                pprintConst(value.value)
            }
            is DeclarationExpression -> {
                val v = if (value != null) {
                    " = ${value.pprint()}"
                } else {
                    ""
                }
                val declType = type
                when (declType) {
                    is Type.Function -> {
                        val ret = declType.type
                        val args = declType.argTypes
                        "${ret.pprint()} $id(${args.map { it.pprint() }.join(", ")})" + when {
                            this is ParameterExpression -> ""
                            else -> ";"
                        }
                    }
                    else -> declType.pprint(id) + " " + v + term()
                }
            }
            is FunctionExpression -> {
                depth++
                try {
                    val list = with(linkedListOf<String>()) {
                        addAll(params.orEmpty().map {
                            it.pprint()
                        })
                        if (vararg != null) {
                            add(/*signature.vararg.toString() +*/ "...")
                        }
                        this
                    }
                    val decl = "${signature.type.pprint()} $id(${list.joinToString(", ")})"
                    return when {
                        children.isEmpty() -> "$decl;"
                        else -> "$decl {\n${children.pprint(append = ";")}\n}"
                    }
                } finally {
                    depth--
                }
            }
            is LoopExpression -> {
                val init = if (initializer != null) initializer.pprint(append = ";") else ""
                val update = if (update != null) update.pprint(append = ";") else ""
                when (checkBefore) {
                    true -> "{\n${init}\nwhile (${predicate.pprint()}) {\n${children.pprint(append = ";")}\n${update}\n}\n}"
                    else -> "{\n${init}\ndo {\n${children.pprint(append = ";")}\n${update}\n} while (${predicate.pprint()});\n}"
                }
            }
            is ReturnStatement -> when {
                returnValue != null -> "return ${returnValue.pprint()}"
                else -> "return"
            }
            is IndexExpression -> {
                "${left.pprint()}[${right.pprint()}]"
            }
            is MemberExpression -> {
                val isEntity = true
                when (isEntity) {
                    true -> "${left.pprint()}[$ns::${field}]"
                    else -> "${left.pprint()}.${field}"
                }
            }
            is BinaryExpression.Eq -> {
                if (left.type(gen) == Type.String && right.type(gen) == Type.String) {
                    "(strcmp(${left.pprint()}, ${right.pprint()}) == 0)"
                } else {
                    "(${left.pprint()} $op ${right.pprint()})"
                }
            }
            is BinaryExpression<*, *> -> if (depth > 0) "(${when (left) {
                is DeclarationExpression -> left.id
                else -> left.pprint()
            }} ${op} ${right.pprint()})" else "/* FIXME: constant fold */"
            is UnaryExpression -> "${op}${operand.pprint()}"
            is BlockExpression -> "{\n${children.pprint(append = ";")}\n}"
            is MethodCallExpression -> "${function.pprint()}(${args.pprint(join = ", ")})"
            is Nop -> ";"
            is ReferenceExpression,
            is BreakStatement,
            is ContinueStatement,
            is GotoExpression,
            is LabelExpression
            -> "$this"
            else -> "/* TODO ${this.javaClass.getSimpleName()} */ $this"
        } + when (this) {
            is BlockExpression,
            is ConditionalExpression -> ""
            else -> term
        }
    }

}


val logger = Logging.new()

val xonotic = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic"

[data] class Project(val root: String, val define: String, val out: String)

val subprojects = listOf(
        Project("menu", "MENUQC", "menuprogs.c")
        , Project("client", "CSQC", "csprogs.c")
        , Project("server", "SVQC", "progs.c")
)
val out = File("out")
val ns = "xon"

fun time(name: String, action: () -> Unit) {
    val start = Date()
    action()
    com.timepath.quakec.compiler.logger.info("$name: ${(Date().getTime() - start.getTime()).toDouble() / 1000} seconds")
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
                val sourceRoot = File("$xonotic/data/xonotic-data.pk3dir/qcsrc/${project.root}")
                val compiler = com.timepath.quakec.compiler.Compiler()
                        .includeFrom(File(sourceRoot, "progs.src"))
                        .define(project.define)
                        .define("QCC_SUPPORT_INT")
                        .define("QCC_SUPPORT_BOOL")

                val ast = compiler.ast()
                val projOut = File(out, project.out)
                projOut.mkdirs()
                val predef = File(projOut, "progs.h")
                FileOutputStream(predef).writer().buffered().use {
                    val predefs = javaClass<CPrinter>().getResourceAsStream("/com/timepath/quakec/compiler/transpiler/predefs.hpp")
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
                    it.write("""cmake_minimum_required(VERSION 2.8)
project(${project.root})
add_executable(${project.root} ${predef.getName()}
${map.keySet().joinToString("\n")})
# target_compile_features(${project.root} PRIVATE cxx_explicit_conversions)
add_definitions(-std=c++11)
""")
                }
                val include = linkedListOf(predef)
                val gen = Generator(CompilerOptions())
                val accumulate = linkedListOf<Expression>()
                for ((f, code) in map) {
                    accumulate.addAll(code)
                    val file = File(projOut, f)
                    val parent = file.getParentFile()
                    parent.mkdirs()
                    val header = /* f.endsWith(".h") */ true;
                    FileOutputStream(file).writer().buffered().use {
                        val pragma = if (header) "#pragma once" else ""
                        it.write("$pragma\n")
                        it.appendln(include.map { "#include \"${parent.toPath().relativize(it.toPath())}\"" }.join("\n"))
                        it.appendln("namespace $ns {")
                        CPrinter(gen, accumulate, ns).pprint(it)
                        it.appendln("}")
                    }
                    if (header)
                        include.add(File(projOut, f))
                }
            }
        }
    }
}
