package com.timepath.compiler.types

public data class Operation(val op: String, val left: Type, val right: Type? = null)
