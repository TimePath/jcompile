package com.timepath.compiler.test

import com.timepath.Logger
import com.timepath.compiler.CompilerOptions
import com.timepath.compiler.ast.*
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.Type
import com.timepath.compiler.PrintVisitor

val logger = Logger.new()

fun main(args: Array<String>) {

    val root = ast {
        val print = const(-1)
        func(Type.Function(Type.Void, emptyList(), null), "test") {
            def("asd", 1)
            add(
                    ConditionalExpression(
                            ConstantExpression(1), true,
                            DeclarationExpression("yay", Type.Float, ConstantExpression(1))
                    )
            )
            add(
                    ConditionalExpression(
                            ConstantExpression(2), true,
                            BlockExpression(listOf(
                                    DeclarationExpression("yay2", Type.Float, ConstantExpression(1))
                            ))
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
        func(Type.Function(Type.Void, emptyList(), null), "test")
        func(Type.Function(Type.Void, emptyList(), null), "main") {
            call(ref("test"))
            ret()
        }
    }
    logger.info(PrintVisitor.render(root))

    logger.info("=======")

    val gen = Generator(CompilerOptions())
    val asm = gen.generate(root.children)

    logger.info("=======")

    logger.info(gen.allocator.toString())

    logger.info("=======")

    logger.info(asm.toString())

    logger.info("=======")

    asm.ir.forEach {
        if (it.real)
            logger.info(it.toString())
    }
}
