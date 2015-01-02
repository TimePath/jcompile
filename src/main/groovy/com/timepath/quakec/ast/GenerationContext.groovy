package com.timepath.quakec.ast

import org.antlr.v4.runtime.misc.Utils

class GenerationContext {
    int g

    def allocate(String id = '') {
        "\$${g++} /* ${Utils.escapeWhitespace(id, false)} */"
    }
}
