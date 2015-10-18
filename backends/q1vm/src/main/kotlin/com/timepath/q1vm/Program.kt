package com.timepath.q1vm

import com.timepath.Logger
import com.timepath.q1vm.util.set
import java.util.*

public class Program(val data: ProgramData) {

    companion object {
        val logger = Logger()
    }

    private val world = data.entities.spawn()

    public fun exec(needle: String = "main"): Unit = exec(data.functions.first { it.name == needle })

    private fun exec(start: ProgramData.Function) {
        class Frame(val func: ProgramData.Function, val comeFrom: Int) {
            private val locals = IntArray(func.numLocals).let {
                data.globalIntData.position(func.firstLocal)
                data.globalIntData.get(it)
                it
            }

            fun restore() {
                data.globalIntData.position(func.firstLocal)
                data.globalIntData.put(locals)
            }
        }

        val stack: Deque<Frame> = linkedListOf(Frame(func = start, comeFrom = -1))
        var stmtIdx = stack.peek().func.firstStatement
        while (stack.isNotEmpty()) {
            val stmt = data.statements[stmtIdx]
            logger.fine { stmt.toString() }
            val skip = stmt(data)
            stmtIdx += skip
            if (skip == 0) {
                // QInstruction.DONE, QInstruction.RETURN
                stack.pop().let {
                    it.restore()
                    stmtIdx = it.comeFrom
                }
                continue
            }
            when (stmt.op) {
                QInstruction.CALL0, QInstruction.CALL1, QInstruction.CALL2, QInstruction.CALL3, QInstruction.CALL4,
                QInstruction.CALL5, QInstruction.CALL6, QInstruction.CALL7, QInstruction.CALL8 -> {
                    val nextFunc = data.functions[data.globalIntData.get(stmt.a)]
                    val nextStmt = nextFunc.firstStatement
                    if (nextStmt < 0) {
                        val id = -nextStmt
                        val paramCount = stmt.op.ordinal() - QInstruction.CALL0.ordinal()
                        invokeBuiltin(id, paramCount)
                    } else {
                        stack.push(Frame(func = nextFunc, comeFrom = stmtIdx))
                        // Copy parameters
                        var i = nextFunc.firstLocal
                        repeat(nextFunc.numParams) { param ->
                            repeat(nextFunc.sizeof[param].toInt()) { ofs ->
                                data.globalIntData[i++] = data.globalIntData.get(QInstruction.OFS_PARAM(param) + ofs)
                            }
                        }
                        stmtIdx = nextStmt
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun invokeBuiltin(id: Int, paramCount: Int) {
        val builtin = builtins[id] ?: throw IndexOutOfBoundsException("Builtin $id not defined")
        builtin.call(this, paramCount).let {
            when (it) {
                is Unit -> Unit
                is Float -> data.globalFloatData.put(QInstruction.OFS_PARAM(-1), it)
                is Int -> data.globalIntData.put(QInstruction.OFS_PARAM(-1), it)
                is String -> data.globalIntData.put(QInstruction.OFS_PARAM(-1), data.strings.tempString(it))
                else -> throw UnsupportedOperationException("Builtin returning ${it.javaClass}")
            }
        }
    }

    public fun KBuiltin(name: String,
                        parameterTypes: Array<Class<*>> = arrayOf(),
                        varargsType: Class<*>? = null,
                        callback: (args: List<*>) -> Any = {}): Builtin =
            Builtin(name, parameterTypes, varargsType, object : Builtin.Handler {
                override fun call(args: List<Any?>): Any = callback(args)
            })

    public val builtins: MutableMap<Int, Builtin> = linkedMapOf(
            1 to KBuiltin("print", varargsType = String::class.java) {
                logger.info { it.map { it.toString() }.join("") }
            },
            2 to KBuiltin("ftos", arrayOf(Float::class.java)) {
                val f = it[0] as Float
                f.toString()
            },
            3 to KBuiltin("spawn") {
                val entityManager = this.data.entities
                entityManager.spawn()
            },
            4 to KBuiltin("kill", arrayOf(Float::class.java)) {
                val e = it[0] as Int
                val entityManager = this.data.entities
                entityManager.kill(e)
            },
            5 to KBuiltin("vtos"),
            6 to KBuiltin("error"),
            7 to KBuiltin("vlen"),
            8 to KBuiltin("etos"),
            9 to KBuiltin("stof", arrayOf(String::class.java)) {
                val s = it[0] as String
                s.toFloat()
            },
            10 to KBuiltin("strcat", varargsType = String::class.java) {
                it.map { it.toString() }.join("")
            },
            11 to KBuiltin("strcmp", arrayOf(String::class.java, String::class.java, Float::class.java)) {
                val first = (it[0] as String).iterator()
                val second = (it[1] as String).iterator()
                var size = if (it.size() == 3) it[2] as Int else -1
                var ret = 0
                while (size-- != 0 && (first.hasNext() || second.hasNext())) {
                    ret = 0
                    if (first.hasNext()) ret += first.next().toInt()
                    if (second.hasNext()) ret -= second.next().toInt()
                    if (ret != 0) break
                }
                ret
            },
            12 to KBuiltin("normalize"),
            13 to KBuiltin("sqrt", arrayOf(Float::class.java)) {
                val n = it[0] as Float
                Math.sqrt(n.toDouble()).toFloat()
            },
            14 to KBuiltin("floor", arrayOf(Float::class.java)) {
                val n = it[0] as Float
                Math.floor(n.toDouble()).toFloat()
            },
            15 to KBuiltin("pow", arrayOf(Float::class.java, Float::class.java)) {
                val base = it[0] as Float
                val exponent = it[1] as Float
                Math.pow(base.toDouble(), exponent.toDouble()).toFloat()
            },
            16 to KBuiltin("assert", arrayOf(Float::class.java, String::class.java)) {
                val assertion = it[0] as Float
                val message = it[1] as String
                if (assertion == 0f) throw AssertionError(message)
            },
            17 to KBuiltin("ftoi", arrayOf(Float::class.java)) {
                val f = it[0] as Float
                f.toInt()
            }
    )
}
