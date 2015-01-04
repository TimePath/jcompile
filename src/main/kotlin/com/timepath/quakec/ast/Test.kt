package com.timepath.quakec.ast

import com.timepath.quakec.ast.impl.BlockStatement
import com.timepath.quakec.ast.impl.FunctionLiteral
import com.timepath.quakec.ast.impl.FunctionCall
import com.timepath.quakec.ast.impl.ConstantExpression
import com.timepath.quakec.ast.impl.BinaryExpression
import com.timepath.quakec.ast.impl.ReturnStatement
import com.timepath.quakec.ast.impl.ReferenceExpression

fun main(args: Array<String>) {
    val root = BlockStatement(// root scope
            FunctionLiteral(
                    "test",
                    Type.Void,
                    array(),
                    BlockStatement(
                            FunctionCall(
                                    ConstantExpression(-1), // print
                                    BinaryExpression.Add(
                                            ConstantExpression(1),
                                            ConstantExpression(2)
                                    )
                            ),
                            ReturnStatement()
                    )
            ),
            FunctionLiteral(
                    "main",
                    Type.Void,
                    array(),
                    BlockStatement(
                            FunctionCall(
                                    ReferenceExpression("test")
                            ),
                            ReturnStatement()
                    )
            )
    )
    println(root.text)
    println("=======")
    val ctx = GenerationContext()
    val asm = root.generate(ctx)
    println(ctx.registry)
    println("=======")
    println(asm)
    asm.forEach {
        if (!it.dummy)
            println(it)
    }
}