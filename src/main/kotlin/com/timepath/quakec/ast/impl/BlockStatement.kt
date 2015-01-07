package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.ast.Statement

class BlockStatement : Statement() {

    override val text: String
        get() = "{\n${children.map { it.text }.join("\n")}\n}"

    override fun generate(ctx: GenerationContext): List<IR> {
        return children.flatMap { it.generate(ctx) }
    }

}
