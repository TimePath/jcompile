package com.timepath.quakec.ast

import com.timepath.quakec.ast.impl.*

def root = new BlockStatement( // root scope
        new FunctionLiteral(
                "test",
                Type.Void,
                (Type[]) [],
                new BlockStatement(
                        new FunctionCall(
                                new ConstantExpression(-1), // print
                                new BinaryExpression.Add(
                                        new ConstantExpression(1),
                                        new ConstantExpression(2)
                                )
                        ),
                        new ReturnStatement()
                )
        ),
        new FunctionLiteral(
                "main",
                Type.Void,
                (Type[]) [],
                new BlockStatement(
                        new FunctionCall(
                                new ReferenceExpression("test"),
                        ),
                        new ReturnStatement()
                )
        )
)
println root.text
println '======='
def ctx = new GenerationContext()
def asm = root.generate(ctx)
def r = ctx.registry
r.reverse.each {
    println """\$${it.key}\t${it.value}\t${r.values[it.key]}"""
}
println '======='
println asm
asm.each {
    if (!it.dummy)
        println it
}
