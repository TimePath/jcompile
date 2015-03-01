package com.timepath.compiler.api

import com.timepath.compiler.TypeRegistry
import com.timepath.compiler.gen.Generator
import com.timepath.compiler.ast.ConstantExpression
import java.util.LinkedHashMap
import com.timepath.compiler.Pointer
import com.timepath.compiler.gen.Allocator
import com.timepath.compiler.CompilerOptions

data class CompileState(val opts: CompilerOptions = CompilerOptions()) {

    val types = TypeRegistry()
    val allocator = Allocator(opts)
    val gen = Generator(this)

    trait FieldCounter {
        fun get(name: String): ConstantExpression
        fun contains(name: String): Boolean
    }

    val fields = object : FieldCounter {
        val map = LinkedHashMap<String, Int>()
        override fun get(name: String) = ConstantExpression(Pointer(map.getOrPut(name) { map.size() }))
        override fun contains(name: String) = name in map
    }
}
