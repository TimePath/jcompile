package com.timepath.q1vm

class Builtin(val name: String,
              val parameterTypes: Array<Class<*>> = arrayOf(),
              val varargsType: Class<*>? = null,
              val callback: Builtin.Handler) {

    interface Handler {
        fun call(args: List<*>): Any
    }

    fun Program.getFloat(i: Int) = data.globalFloatData.get(i)
    fun Program.getString(i: Int) = data.strings[data.globalIntData.get(i)]

    fun call(ctx: Program, parameterCount: Int): Any {
        var offset = QInstruction.OFS_PARAM(0)
        fun read(it: Any): Any? = when (it) {
            Float::class.java -> {
                val i = offset
                offset += 3
                ctx.getFloat(i)
            }
            String::class.java -> {
                val i = offset
                offset += 3
                ctx.getString(i)
            }
            else -> it
        }

        val args: MutableList<Any?> = arrayListOf()
        parameterTypes.mapTo(args) { read(it) }
        if (varargsType != null)
            (parameterTypes.size..parameterCount - 1).mapTo(args) { read(varargsType) }
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
