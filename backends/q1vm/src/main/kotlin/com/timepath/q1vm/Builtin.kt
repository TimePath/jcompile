package com.timepath.q1vm

import java.util.ArrayList

class Builtin(val name: String,
              val parameterTypes: Array<Class<*>> = array(),
              val varargsType: Class<*>? = null,
              val callback: Builtin.Handler) {

    trait Handler {
        fun call(args: List<*>): Any
    }

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
        //        Program.logger.info("""$name(${
        //        args.map({
        //            if (it is String)
        //                it.quote()
        //            else
        //                it.toString()
        //        }).join(", ")
        //        })""")
        return callback.call(args)
    }
}
