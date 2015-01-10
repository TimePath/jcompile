package com.timepath.quakec.vm

import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.FloatBuffer
import kotlin.properties.Delegates
import com.timepath.quakec.vm.ProgramData.Header
import com.timepath.quakec.vm.ProgramData.Header.Section

fun Statement(op: Short, a: Short, b: Short, c: Short): Statement = Statement(Instruction.from(op.toInt()), a.toInt(), b.toInt(), c.toInt())
class Statement(val op: Instruction, val a: Int, val b: Int, val c: Int) {

    var data: ProgramData? = null

    fun invoke(data: ProgramData): Int {
        return op(this, data)
    }

    override fun toString(): String {
        return op.toString(this, data)
    }

}

class Definition(val type: Short,
                 val offset: Short,
                 val nameOffset: Int) {

    var data: ProgramData? = null

    val name: String?
        get() = data?.strings!![nameOffset]

    override fun toString(): String = """Definition {
    type=$type,
    offset=$offset,
    name=$name
}"""

}

class Function(val firstStatement: Int,
               val firstLocal: Int,
               val numLocals: Int,
               val profiling: Int,
               val nameOffset: Int,
               val fileNameOffset: Int,
               val numParams: Int,
               val sizeof: ByteArray) {

    var data: ProgramData? = null

    val name: String?
        get() = data?.strings!![this.nameOffset]

    val fileName: String?
        get() = data?.strings!![this.fileNameOffset]

    override fun toString(): String = """Function {
    firstStatement=${firstStatement},
    firstLocal=${firstLocal},
    numLocals=${numLocals},
    profiling=${profiling},
    name=${name},
    fileName=${fileName},
    numParams=${numParams},
    sizeof=${sizeof}
}"""
}

data class ProgramData(val header: Header? = null,
                       val statements: List<Statement>? = null,
                       val globalDefs: List<Definition>? = null,
                       val fieldDefs: List<Definition>? = null,
                       val functions: List<Function>? = null,
                       val strings: StringManager? = null,
                       val globalData: ByteBuffer? = null) {

    val entities = EntityManager(this)

    data class Header(
            /**
             * Latest version: 6
             */
            val version: Int,
            /**
             * CRC16
             */
            val crc: Int,
            val statements: Section,
            val globalDefs: Section,
            val fieldDefs: Section,
            val functions: Section,
            val stringData: Section,
            val globalData: Section,
            val entityCount: Int) {

        data class Section(
                /**
                 * Absolute offset in the progs file
                 */
                val offset: Int,
                /**
                 * Number of records in the section
                 */
                val count: Int)
    }

    val globalIntData: IntBuffer by Delegates.lazy {
        globalData!!.asIntBuffer()
    }
    val globalFloatData: FloatBuffer by Delegates.lazy {
        globalData!!.asFloatBuffer()
    }
}
