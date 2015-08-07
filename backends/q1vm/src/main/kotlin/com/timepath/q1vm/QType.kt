package com.timepath.q1vm

import com.timepath.compiler.backend.q1vm.types.*
import com.timepath.compiler.types.Type
import com.timepath.compiler.types.defaults.function_t

enum class QType {
    Void,
    String,
    Float,
    Vector,
    Entity,
    Field,
    Function,
    Pointer;

    companion object {
        fun get(it: Type) = when (it) {
            string_t -> QType.String
            is number_t -> QType.Float
            is class_t -> QType.Entity
            is field_t -> QType.Field
            is function_t -> QType.Function
            is array_t -> QType.Function
            else -> QType.Float
        }
    }
}
