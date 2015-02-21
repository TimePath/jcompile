package com.timepath.compiler.types

import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.ast.StructDeclarationExpression

data abstract class struct_t(val fields: Map<String, Type>) : Type {
    override fun declare(name: String, value: ConstantExpression?): List<DeclarationExpression> {
        return listOf(StructDeclarationExpression(name, this))
    }
}
