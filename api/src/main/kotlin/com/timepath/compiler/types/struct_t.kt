package com.timepath.compiler.types

import com.timepath.compiler.ast.ConstantExpression
import com.timepath.compiler.ast.DeclarationExpression
import com.timepath.compiler.ast.StructDeclarationExpression
import com.timepath.compiler.api.CompileState

data abstract class struct_t(vararg fields: Pair<String, Type>) : Type {
    val fields = linkedMapOf(*fields)
    override val simpleName = "struct_t"
    override fun declare(name: String, value: ConstantExpression?, state: CompileState?): List<DeclarationExpression> {
        return listOf(StructDeclarationExpression(name, this))
    }
}
