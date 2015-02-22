package com.timepath.compiler.api

import com.timepath.compiler.TypeRegistry
import com.timepath.compiler.gen.Generator

data class CompileState(
        val types: TypeRegistry = TypeRegistry(),
        val gen: Generator
)
