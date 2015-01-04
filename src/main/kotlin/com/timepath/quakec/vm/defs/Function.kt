package com.timepath.quakec.vm.defs

class Function(val firstStatement: Int,
               val firstLocal: Int,
               val numLocals: Int,
               val profiling: Int,
               val nameOffset: Int,
               val fileNameOffset: Int,
               val numParams: Int,
               val sizeof: ByteArray) {

    val data: ProgramData? = null

    val name: String
        get() = data!!.strings!![this.nameOffset]

    val fileName: String
        get() = data!!.strings!![this.fileNameOffset]

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



