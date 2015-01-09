package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Statement
import com.timepath.quakec.ast.Expression

/**
 * Return can be assigned to, and has a constant address
 */
class ReturnStatement(val returnValue: Expression?) : Statement() {

}
