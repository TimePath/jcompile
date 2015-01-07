package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.GenerationContext
import com.timepath.quakec.ast.IR
import com.timepath.quakec.ast.Statement
import com.timepath.quakec.vm.Instruction
import com.timepath.quakec.ast.Expression

/**
 * Return can be assigned to, and has a constant address
 */
class ReturnStatement(val returnValue: Expression?) : Statement() {

    override val text: String
        get() = "return '\$1 \$2 \$3'"

    override fun generate(ctx: GenerationContext): List<IR> {
        return listOf(IR(Instruction.RETURN, array(0, 0, 0), 0))
    }

}
