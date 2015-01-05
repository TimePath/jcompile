package com.timepath.quakec.vm

import com.timepath.quakec.vm.defs.Function
import com.timepath.quakec.vm.defs.ProgramData

import java.io.File
import java.util.*
import org.antlr.v4.runtime.misc.Utils

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
                    data!!.globalIntData.put(k++, data.globalIntData.get(Instruction.OFS_PARM0 + (3 * i) + j))
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
                        val parameterTypes: Array<Class<*>>,
                        val varargsType: Class<*>?,
                        val callback: (args: List<*>) -> Unit) {

        fun call(parameterCount: Int): Any? {
            var offset = Instruction.OFS_PARM0
            val getFloat = {(i: Int) -> data!!.globalFloatData.get(i) }
            val getString = {(i: Int) -> data!!.strings!![data.globalIntData.get(i)] }
            val read = {(it: Any) ->
                when (it) {
                    is Float -> {
                        offset += 3
                        getFloat(offset - 3)
                    }
                    is String -> {
                        offset += 3
                        getString(offset - 3)
                    }
                    else -> null
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
                        it.map { it.toString() }.join("")
                    }
            ),
            2 to Builtin(
                    name = "ftos",
                    parameterTypes = array(javaClass<Float>()),
                    varargsType = null,
                    callback = {
                        it[0].toString()
                    }
            )
    )

    fun builtin(id: Int, parameterCount: Int) {
        val builtin = builtins[id]
        val ret = builtin?.call(parameterCount)
        if (ret == null) return

        when (ret) {
            is Float -> data!!.globalFloatData.put(Instruction.OFS_RETURN, ret)
            is String -> {
                // TODO: make temp string
            }
        }
    }

}

fun main(args: Array<String>) {
    val data = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic/gmqcc"
    Program(ProgramDataReader(File("$data/progs.dat")).read()).exec()
}
