package com.timepath.quakec.vm

import java.util.ArrayList
import com.timepath.quakec.compiler.quote

fun KBuiltin(name: String,
            parameterTypes: Array<Class<*>> = array(),
            varargsType: Class<*>? = null,
            callback: (args: List<*>) -> Any = {}) =
        Builtin(name, parameterTypes, varargsType, object : BuiltinHandler {
            override fun call(args: List<Any?>): Any = callback(args)
        })

trait BuiltinHandler {
    fun call(args: List<*>): Any
}

class Builtin(val name: String,
              val parameterTypes: Array<Class<*>> = array(),
              val varargsType: Class<*>? = null,
              val callback: BuiltinHandler) {

    fun call(ctx: Program, parameterCount: Int): Any {
        var offset = Instruction.OFS_PARAM(0)
        fun read(it: Any): Any? = when (it) {
            javaClass<Float>() -> {
                val i = offset
                offset += 3
                ctx.getFloat(i)
            }
            javaClass<String>() -> {
                val i = offset
                offset += 3
                ctx.getString(i)
            }
            else -> it
        }

        val args: MutableList<Any?> = ArrayList(parameterCount)
        parameterTypes.mapTo(args) { read(it) }
        if (varargsType != null)
            (parameterTypes.size()..parameterCount - 1).mapTo(args) { read(varargsType) }
        Program.logger.info("""$name(${
        args.map({
            if (it is String)
                it.quote()
            else
                it.toString()
        }).join(", ")
        })""")
        return callback.call(args)
    }
}
