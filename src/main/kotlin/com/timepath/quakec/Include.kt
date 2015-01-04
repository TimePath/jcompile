package com.timepath.quakec

import org.anarres.cpp.FileLexerSource
import org.anarres.cpp.Source
import org.anarres.cpp.StringLexerSource

import java.io.File

fun Include(file: File): Include = Include(file.name, file.canonicalPath, FileLexerSource(file))
fun Include(input: String, name: String): Include = Include(name, name, StringLexerSource(input))

data class Include(val name: String, val path: String, val source: Source)
