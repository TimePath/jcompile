package com.timepath.quakec.ast

trait Statement {

    /**
     * @return Replacement node
     */
    def generate() { null }

    abstract String getText()
}

