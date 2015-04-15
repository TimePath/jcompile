package com.timepath.compiler.types

class TypeRegistry {

    private val types = linkedMapOf<String, Type>()

    fun get(name: String): Type {
        return types[name]
    }

    fun set(name: String, t: Type) {
        types[name] = t
    }
}
