package com.timepath.quakec.ast

import com.timepath.quakec.ast.impl.BinaryExpression
import com.timepath.quakec.ast.impl.DeclarationExpression

fun main(args: Array<String>) {

    val root = ast {
        val print = const(-1)
        func(Type.Void, "test", array()) {
            def("asd")
            call(print) {
                arg(BinaryExpression.Add(
                        ref("asd"),
                        const(2)
                ))
                arg(const(4))
                arg(BinaryExpression.Add(
                        const(1),
                        const(2)
                ))
                arg(BinaryExpression.Add(
                        const(3),
                        const(4)
                ))
                arg(BinaryExpression.Add(
                        BinaryExpression.Add(
                                const(1),
                                const(2)
                        ),
                        const(4)
                ))
            }
            ret()
        }
        func(Type.Void, "test", array())
        func(Type.Void, "main", array()) {
            call(ref("test"))
            ret()
        }
    }
    println(root.toStringRecursive())

    println("=======")

    val ctx = GenerationContext(root)
    val asm = ctx.generate()

    println("=======")

    println(ctx.registry)

    println("=======")

    println(asm)

    println("=======")

    asm.forEach {
        if (!it.dummy)
            println(it)
    }
}