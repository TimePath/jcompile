package com.timepath.quakec

import groovy.transform.CompileStatic
import org.anarres.cpp.FileLexerSource
import org.anarres.cpp.Source
import org.anarres.cpp.StringLexerSource

@CompileStatic
class Include {

    String name, path
    Source source

    Include(File file) {
        name = file.name
        path = file.canonicalPath
        source = new FileLexerSource(file)
    }

    Include(String input, String name) {
        this.name = name
        path = name
        source = new StringLexerSource(input)
    }

}
