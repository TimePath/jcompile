package com.timepath.quakec.compiler.ast

class ConstantExpression(any: Any) : Expression() {

    val value = Value(any)

    override val attributes: Map<String, Any?>
        get() = mapOf("value" to value)

    override fun evaluate(): Value = value

    override fun toString(): String = value.toString()

}

open class ReferenceExpression(val id: String) : Expression() {

    override val attributes: Map<String, Any>
        get() = mapOf("id" to id)

    override fun toString(): String = id

}

class DeclarationExpression(id: String) : ReferenceExpression(id)
