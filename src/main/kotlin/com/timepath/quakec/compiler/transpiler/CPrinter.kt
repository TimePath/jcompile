/**
 * Transpiles QuakeC to regular C
 */
package com.timepath.quakec.compiler.transpiler

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import com.timepath.quakec.Logging
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
import com.timepath.quakec.compiler.ast.LabelExpression
import com.timepath.quakec.compiler.ast.LoopExpression
import com.timepath.quakec.compiler.ast.MemberExpression
import com.timepath.quakec.compiler.ast.MemoryReference
import com.timepath.quakec.compiler.ast.MethodCallExpression
import com.timepath.quakec.compiler.ast.ReferenceExpression
import com.timepath.quakec.compiler.ast.ReturnStatement
import com.timepath.quakec.compiler.ast.SwitchExpression
import com.timepath.quakec.compiler.ast.SwitchExpression.Case
import com.timepath.quakec.compiler.ast.UnaryExpression
import com.timepath.quakec.compiler.ast.Nop
import org.antlr.v4.runtime.misc.Utils

class CPrinter(val all: List<Expression>) {

    var depth: Int = 0

    fun pprint(out: BufferedWriter) {
        all.forEach { out.appendln(it.pprint()) }
    }

    fun List<Expression>.pprint(append: String = "", join: String = "\n", action: (it: Expression) -> String = { it.pprint() }): String {
        return map { action(it) + append }.joinToString(join)
    }

    fun Type.pprint(id: kotlin.String? = null, indirection: Int = 0): String {
        val stars = "*".repeat(indirection)
        return when (this) {
            is Type.Function -> "${type.pprint()}($stars*$id)(${argTypes.map {
                it.pprint()
            }.joinToString(", ")})"
            is Type.Field -> "${type.pprint(id, indirection + 1)}"
            else -> {
                when (id) {
                    null -> "$this$stars"
                    else -> "$this $stars$id"
                }
            }
        }
    }

    fun term(): String = if (depth == 0) ";" else ""

    fun Expression.pprint(term: String = ""): String {
        return /*"/* ${this.javaClass.getSimpleName()} */" +*/ when (this) {
            is ConditionalExpression -> {
                if (expression)
                    "(${test.pprint()} ? ${pass.pprint()} : ${fail!!.pprint()})"
                else
                    "if (${test.pprint()})\n${pass.pprint(";")}" + if (fail == null) "" else "\nelse\n${fail.pprint(";")}"
            }
            is ConstantExpression -> {
                val v = value.value
                when (v) {
                    null -> "NULL"
                    is Array<*> -> "(vector) { ${v.joinToString(", ")} }"
                    is String -> '"' + Utils.escapeWhitespace(v, false)/*.replace("\"", "\\\"")*/ + '"'
                    is Float -> v.toString() + "f"
                    else -> "$v"
                }
            }
            is DeclarationExpression -> {
                val v = if (value != null) {
                    " = ${value.pprint()}"
                } else {
                    ""
                }
                when (type) {
                    is Type.Function -> {
                        val ret = type.type
                        val args = type.argTypes
                        "${ret.pprint()} $id(${args.map { it.pprint() }.join(", ")});"
                    }
                    else -> type.pprint(id) + " " + v + term()
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
                "{\n${init}\nwhile (${predicate.pprint()}) {\n${children.pprint(append = ";")}\n${update}\n}\n}"
            }
            is ReturnStatement -> when {
                returnValue != null -> "return ${returnValue.pprint()}"
                else -> "return"
            }
            is MemoryReference -> "\"$$ref\""
            is SwitchExpression -> "switch (${test.pprint()}) ${children.pprint()}"
            is MemberExpression -> "${left.pprint()}[${right.pprint()}]"
            is BinaryExpression<*, *> -> if (depth > 0) "(${when (left) {
                is DeclarationExpression -> left.id
                else -> left.pprint()
            }} ${op} ${right.pprint()})" else "/* FIXME: constant fold */"
            is UnaryExpression -> "${op}${operand.pprint()}"
            is BlockExpression -> "{\n${children.pprint(append = ";")}\n}"
            is MethodCallExpression -> "${function.pprint()}(${args.pprint(join = ", ")})"
            is Case -> when (expr) {
                null -> "default"
                else -> "case ${expr.pprint()}"
            } + ":"
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

fun time(name: String, action: () -> Unit) {
    val start = Date()
    action()
    com.timepath.quakec.compiler.logger.info("$name: ${(Date().getTime() - start.getTime()).toDouble() / 1000} seconds")
}

fun main(args: Array<String>) {
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

                val ast = compiler.ast()
                val projOut = File(out, project.out)
                projOut.mkdirs()
                val predef = File(projOut, "progs.h")
                FileOutputStream(predef).writer().buffered().use {
                    it.write("""
#pragma once
typedef const char *string;

inline string _(string str) {
    return str;
}

struct vector {
    float x, y, z;

    vector operator+(const vector &other) const {
        return (vector) {x + other.x, y + other.y, z + other.z};
    }

    vector &operator+=(const vector &other) {
        vector self = *this;
        self.x += other.x;
        self.y += other.y;
        self.z += other.z;
        return self;
    }

    vector operator-(const vector &other) const {
        return (vector) {x - other.x, y - other.y, z - other.z};
    }

    vector &operator-=(const vector &other) {
        vector self = *this;
        self.x -= other.x;
        self.y -= other.y;
        self.z -= other.z;
        return self;
    }

    vector operator*(const float other) const {
        return (vector) {x * other, y * other, z * other};
    }

    vector &operator*=(const float other) {
        vector self = *this;
        self.x *= other;
        self.y *= other;
        self.z *= other;
        return self;
    }

    vector operator/(const float other) const {
        return (vector) {x / other, y / other, z / other};
    }

    vector &operator/=(const float other) {
        vector self = *this;
        self.x /= other;
        self.y /= other;
        self.z /= other;
        return self;
    }
};

vector operator*(float f, const vector &other) const {
    return (vector) {f * other.x, f * other.y, f * other.z};
}

struct entity {

    template<typename T>
    T &operator[](T *field) {
        return T();
    }

};
""")
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
""")
                }
                val include = linkedListOf(predef)
                for ((f, code) in map) {
                    val file = File(projOut, f)
                    file.getParentFile().mkdirs()
                    val header = /* f.endsWith(".h") */ true;
                    FileOutputStream(file).writer().buffered().use {
                        val pragma = if (header) "#pragma once" else ""
                        it.write("$pragma\n")
                        it.appendln(include.map { "#include \"${it.getAbsolutePath()}\"" }.join("\n"))
                        CPrinter(code).pprint(it)
                    }
                    if (header)
                        include.add(File(projOut, f))
                }
            }
        }
    }
}
