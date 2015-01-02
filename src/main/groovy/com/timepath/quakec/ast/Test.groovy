package com.timepath.quakec.ast

import com.timepath.quakec.ast.impl.*

def root = new BlockStatement(
        new BinaryExpression.Assign(
                new DeclarationExpression(Type.String, "global"),
                new ConstantExpression("Hello")
        ),
        new BinaryExpression.Assign(
                new DeclarationExpression(Type.Function, "main0"),
                new FunctionLiteral(
                        Type.Void,
                        (Type[]) [],
                        new BlockStatement(
                                new BinaryExpression.Assign(
                                        new ReferenceExpression("temp"),
                                        new ConditionalExpression(
                                                new ConstantExpression(1),
                                                new ReferenceExpression("global"),
                                                new BinaryExpression.Add(
                                                        new ConstantExpression(0),
                                                        new ConstantExpression(7)
                                                )
                                        )
                                ),
                                new FunctionCall(
                                        new ReferenceExpression("print"),
                                        new ReferenceExpression("temp"),
                                        new ConstantExpression("\n")
                                ),
                        )
                )
        ),
        new BinaryExpression.Assign(
                new DeclarationExpression(Type.Function, "main"),
                new ReferenceExpression("main0")
        )
)
println root.text