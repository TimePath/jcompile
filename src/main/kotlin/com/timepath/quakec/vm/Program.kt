package com.timepath.quakec.vm

import java.io.File
import java.util.*
import org.antlr.v4.runtime.misc.Utils
import com.timepath.quakec.vm.util.ProgramDataReader

public class Program(val data: ProgramData?) {

    public fun exec(needle: String = "main") {
        exec(data!!.functions!!.first { it.name == needle })
    }

    data class Frame(val sp: Int,
                     val stmt: Int,
                     val fn: Function?)

    fun exec(start: Function) {
        val stack = Stack <Frame> ()
        var sp = -1
        var stmt = -1
        var fn: Function? = null
        val push = {(it: Function) ->
            stack add Frame(sp, stmt, fn)

            // TODO: Store locals to support recursion

            // Copy parameters

            var k = it.firstLocal
            for (i in 0..it.numParams - 1) {
                for (j in 0..it.sizeof[i] - 1) {
                    data!!.globalIntData.put(k++, data.globalIntData.get(Instruction.OFS_PARAM(i) + j))
                }
            }

            sp = 0
            stmt = it.firstStatement
            fn = it
        }
        val pop = {
            val it = stack.pop()

            // TODO: Pop locals

            sp = it.sp
            stmt = it.stmt
            fn = it.fn
        }

        push(start)

        while (!stack.empty()) {
            val s = data!!.statements!![stmt]
            println(s.toString())

            val ret = s(data).toInt()
            if (ret == 0) {
                // DONE, RETURN
                pop()
                continue
            }

            stmt += ret
            when (s.op) {
                Instruction.CALL0, Instruction.CALL1, Instruction.CALL2, Instruction.CALL3, Instruction.CALL4, Instruction.CALL5, Instruction.CALL6, Instruction.CALL7, Instruction.CALL8 -> {
                    val function = data.functions!![data.globalIntData.get(s.a.toInt())]
                    val i = function.firstStatement
                    if (i < 0) {
                        builtin(-i, s.op.ordinal() - Instruction.CALL0.ordinal())
                    } else {
                        push(function)
                    }
                }
            }
        }

    }

    inner class Builtin(val name: String,
                        val parameterTypes: Array<Class<*>> = array(),
                        val varargsType: Class<*>? = null,
                        val callback: (args: List<*>) -> Any = {}) {

        fun call(parameterCount: Int): Any {
            var offset = Instruction.OFS_PARAM(0)
            val getFloat = {(i: Int) -> data!!.globalFloatData.get(i) }
            val getString = {(i: Int) -> data!!.strings!![data.globalIntData.get(i)] }
            val read = {(it: Any) ->
                when (it) {
                    javaClass<Float>() -> {
                        val i = offset
                        offset += 3
                        getFloat(i)
                    }
                    javaClass<String>() -> {
                        val i = offset
                        offset += 3
                        getString(i)
                    }
                    else -> it
                }
            }
            val args: MutableList<*> = ArrayList(parameterCount)
            args addAll parameterTypes.map { read(it) }
            if (varargsType != null)
                args addAll ((parameterTypes.size()..parameterCount - 1).map { read(varargsType) })
            println("""$name(${
            args.map({
                if (it is String)
                    "\"${Utils.escapeWhitespace(it, false)}\""
                else
                    it.toString()
            }).join(", ")
            })""")
            return callback(args)
        }
    }

    val builtins: Map<Int, Builtin> = mapOf(
            1 to Builtin(
                    name = "print",
                    parameterTypes = array(),
                    varargsType = javaClass<String>(),
                    callback = {
                        println(it.map { it.toString() }.join(""))
                    }
            ),
            2 to Builtin(
                    name = "ftos",
                    parameterTypes = array(javaClass<Float>()),
                    varargsType = null,
                    callback = {
                        val f = it[0] as Float
                        f.toString()
                    }
            ),
            3 to Builtin(
                    name = "spawn"
            ),
            4 to Builtin(
                    name = "kill"
            ),
            5 to Builtin(
                    name = "vtos"
            ),
            6 to Builtin(
                    name = "error"
            ),
            7 to Builtin(
                    name = "vlen"
            ),
            8 to Builtin(
                    name = "etos"
            ),
            9 to Builtin(
                    name = "stof",
                    parameterTypes = array(javaClass<String>()),
                    varargsType = null,
                    callback = {
                        val s = it[0] as String
                        s.toFloat()
                    }
            ),
            10 to Builtin(
                    name = "strcat",
                    parameterTypes = array(),
                    varargsType = javaClass<String>(),
                    callback = {
                        it.map { it.toString() }.join("")
                    }
            ),
            11 to Builtin(
                    name = "strcmp",
                    parameterTypes = array(javaClass<String>(), javaClass<String>(), javaClass<Float>()),
                    varargsType = null,
                    callback = {
                        val first = (it[0] as String).iterator()
                        val second = (it[1] as String).iterator()
                        var size = if (it.size() == 3) it[2] as Int else -1
                        var ret = 0
                        while (size-- != 0 && (first.hasNext() || second.hasNext())) {
                            ret = 0
                            if (first.hasNext())
                                ret += first.next()
                            if (second.hasNext())
                                ret -= second.next()
                            if (ret != 0)
                                break
                        }
                        ret
                    }
            ),
            12 to Builtin(
                    name = "normalize"
            ),
            13 to Builtin(
                    name = "sqrt",
                    parameterTypes = array(javaClass<Float>()),
                    varargsType = null,
                    callback = {
                        val n = it[0] as Float
                        Math.sqrt(n.toDouble()).toFloat()
                    }
            ),
            14 to Builtin(
                    name = "floor",
                    parameterTypes = array(javaClass<Float>()),
                    varargsType = null,
                    callback = {
                        val n = it[0] as Float
                        Math.floor(n.toDouble()).toFloat()
                    }
            ),
            15 to Builtin(
                    name = "pow",
                    parameterTypes = array(javaClass<Float>(), javaClass<Float>()),
                    varargsType = null,
                    callback = {
                        val base = it[0] as Float
                        val exponent = it[1] as Float
                        Math.pow(base.toDouble(), exponent.toDouble()).toFloat()
                    }
            )
    )

    fun builtin(id: Int, parameterCount: Int) {
        val builtin = builtins[id]
        val ret = builtin?.call(parameterCount)
        if (ret == null) return

        data!!
        when (ret) {
            is Float -> {
                data.globalFloatData.put(Instruction.OFS_PARAM(-1), ret)
            }
            is String -> {
                data.strings!!
                data.globalIntData.put(Instruction.OFS_PARAM(-1), data.strings.tempString(ret))
            }
        }
    }

}

fun main(args: Array<String>) {
    val data = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic/gmqcc"
    Program(ProgramDataReader(File("$data/progs.dat")).read()).exec()
}