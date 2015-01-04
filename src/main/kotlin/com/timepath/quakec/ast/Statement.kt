package com.timepath.quakec.ast

import java.util.LinkedList

trait Statement {
    /**
     * @return Nested lists, the last argument of which is an rvalue and is removed
     */
    fun generate(ctx: GenerationContext): List<IR> = LinkedList()

    val text: String
}
