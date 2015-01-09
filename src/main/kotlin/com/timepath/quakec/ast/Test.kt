package com.timepath.quakec.ast

import com.timepath.quakec.ast.impl.BinaryExpression
import com.timepath.quakec.ast.impl.DeclarationExpression
import com.timepath.quakec.ast.impl.ConditionalExpression
import com.timepath.quakec.ast.impl.ConstantExpression

fun main(args: Array<String>) {

    val root = ast {
        val print = const(-1)
        func(Type.Void, "test", array()) {
            def("asd")
            add(
                    ConditionalExpression(
                            ConstantExpression(1),
                            DeclarationExpression("yay")
                    )
            )
            add(
                    ConditionalExpression(
                            ConstantExpression(2),
                            BlockStatement(
                                    DeclarationExpression("yay2")
                            )
                    )
            )
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

    val ctx = GenerationContext(root.children)
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