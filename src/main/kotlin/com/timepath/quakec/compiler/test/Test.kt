package com.timepath.quakec.compiler.test

import com.timepath.quakec.Logging
import com.timepath.quakec.compiler.CompilerOptions
import com.timepath.quakec.compiler.ast.*
import com.timepath.quakec.compiler.gen.Generator
import com.timepath.quakec.compiler.Type

val logger = Logging.new()

fun main(args: Array<String>) {

    val root = ast {
        val print = const(-1)
        func(Type.Void, "test", array()) {
            def("asd", 1)
            add(
                    ConditionalExpression(
                            ConstantExpression(1),
                            DeclarationExpression("yay", ConstantExpression(1))
                    )
            )
            add(
                    ConditionalExpression(
                            ConstantExpression(2),
                            BlockExpression(listOf(
                                    DeclarationExpression("yay2", ConstantExpression(1))
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
        func(Type.Void, "test", array())
        func(Type.Void, "main", array()) {
            call(ref("test"))
            ret()
        }
    }
    logger.info(root.toStringRecursive())

    logger.info("=======")

    val ctx = Generator(CompilerOptions(), root.children)
    val asm = ctx.generate()

    logger.info("=======")

    logger.info(ctx.allocator.toString())

    logger.info("=======")

    logger.info(asm.toString())

    logger.info("=======")

    asm.forEach {
        if (it.real)
            logger.info(it.toString())
    }
}