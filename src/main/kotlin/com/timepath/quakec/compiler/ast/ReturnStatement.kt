package com.timepath.quakec.compiler.ast

/**
 * Return can be assigned to, and has a constant address
 */
class ReturnStatement(val returnValue: Expression?) : Statement()