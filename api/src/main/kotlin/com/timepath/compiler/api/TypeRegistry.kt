package com.timepath.compiler.api

import com.timepath.compiler.types.Type

public interface TypeRegistry {
    fun get(name: String): Type?
    fun set(name: String, t: Type)
}
