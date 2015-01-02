package com.timepath.quakec.ast

trait Statement {

    /**
     * @return Nested lists, the last argument of which is an rvalue and is removed
     */
    def generate(GenerationContext ctx) { null }

    abstract String getText()
}

