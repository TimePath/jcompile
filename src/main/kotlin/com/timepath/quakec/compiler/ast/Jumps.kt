package com.timepath.quakec.compiler.ast

/**
 * Return can be assigned to, and has a constant address
 */
class ReturnStatement(val returnValue: Expression?) : Statement()

// TODO: labels
class ContinueStatement() : Statement()
class BreakStatement() : Statement()
class GotoStatement() : Statement()