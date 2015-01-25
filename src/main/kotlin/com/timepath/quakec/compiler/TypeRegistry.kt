package com.timepath.quakec.compiler

class TypeRegistry {

    private val types = linkedMapOf<String, Type>()

    fun get(name: String) = types[name]
    fun set(name: String, t: Type) {
        types[name] = t
    }

    {
        set("void", Type.Void)
        set("float", Type.Float)
        set("vector", Type.Vector)
        set("string", Type.String)
        set("entity", Type.Entity)
        set("int", Type.Int)
        set("bool", Type.Bool)
    }

}
