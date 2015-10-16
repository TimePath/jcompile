package com.timepath.compiler.api

import com.timepath.compiler.types.Type

public interface TypeRegistry {
    operator fun get(name: String): Type?
    operator fun set(name: String, t: Type)
}
