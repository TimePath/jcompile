package com.timepath.compiler

import com.timepath.compiler.types.*

class TypeRegistry {

    private val types = linkedMapOf<String, Type>()

    fun get(name: String): Type {
        return types[name]
    }

    fun set(name: String, t: Type) {
        types[name] = t
    }

    {
        set("void", void_t)
        set("float", float_t)
        set("vector", vector_t)
        set("string", string_t)
        set("entity", entity_t)
        set("int", int_t)
        set("bool", bool_t)
    }

}
