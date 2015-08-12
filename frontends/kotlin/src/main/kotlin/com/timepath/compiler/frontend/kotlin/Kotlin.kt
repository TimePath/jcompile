package com.timepath.compiler.frontend.kotlin

import com.intellij.openapi.Disposable
import com.intellij.psi.PsiFileFactory
import com.timepath.Printer
import com.timepath.compiler.Compiler
import com.timepath.compiler.api.CompileState
import com.timepath.compiler.api.Frontend
import com.timepath.compiler.ast.Expression
import com.timepath.with
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetVisitor

public class Kotlin : Frontend<CompileState, Sequence<List<Expression>>> {
    override fun parse(includes: List<Compiler.Include>, state: CompileState) = throw UnsupportedOperationException()
    override fun define(name: String, value: String) = throw UnsupportedOperationException()
}

fun main(args: Array<String>) {
    val env = KotlinCoreEnvironment.createForProduction(Disposable { }, CompilerConfiguration(), arrayListOf())
    val file = PsiFileFactory.getInstance(env.project)
            .createFileFromText("test.kt", JetFileType.INSTANCE, """
fun main(args: Array<String>) {
    println()
}
""")
    // TODO: ExpressionVisitor extends TranslatorVisitor<JsNode>
    file.accept(object : JetVisitor<Void?, Printer>() {
        val self = this
        override fun visitJetElement(element: JetElement, data: Printer) = data.with {
            +"  " {
                +element
                element.acceptChildren(self, this)
            }
        } let { null }

        override fun visitJetFile(file: JetFile, data: Printer?) = Printer {
            +file
            file.acceptChildren(self, this)
        }.let { println(it) } let { null }
    })
}
