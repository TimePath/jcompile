package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR

class DeclarationExpression(id: String) : ReferenceExpression(id) {

    override val attributes: Map<String, Any>
        get() = mapOf("id" to id)

}
