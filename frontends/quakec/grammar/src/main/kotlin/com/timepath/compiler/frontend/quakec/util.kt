package com.timepath.compiler.frontend.quakec

import kotlin.properties.Delegates
import kotlin.properties.ReadOnlyProperty

inline fun named<T : Any>(
        @inlineOptions(InlineOption.ONLY_LOCAL_RETURN)
        f: NamedObject<T>.() -> T
): NamedObject<T> = object : NamedObject<T>() {
    override val it = f()
}

private abstract class NamedObject<T> : ReadOnlyProperty<Any?, T> {
    var name: String by Delegates.notNull()
        private set

    abstract val it: T

    override fun get(thisRef: Any?, desc: PropertyMetadata): T {
        name = desc.name
        return it
    }

    public companion object {
        fun get<T>(inst: Any): List<T> {
            val clazz = inst.javaClass
            val metadata = clazz.getDeclaredField("\$propertyMetadata").let {
                it.setAccessible(true)
                it.get(inst) as Array<PropertyMetadata>
            }
            return metadata.map @suppress("UNCHECKED_CAST") {
                val fld = clazz.getDeclaredField("${it.name}\$delegate")
                fld.setAccessible(true)
                val obj = fld.get(inst) as? NamedObject<T>
                obj?.get(inst, it)
                obj
            }.filterNotNull().map { it.it }
        }
    }
}
