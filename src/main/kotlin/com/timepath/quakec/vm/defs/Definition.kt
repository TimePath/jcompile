package com.timepath.quakec.vm.defs

class Definition(val type: Short,
                 val offset: Short,
                 val nameOffset: Int) {

    var data: ProgramData? = null

    val name: String
        get() = data?.strings!![nameOffset]

    override fun toString(): String = """Definition {
    type=$type,
    offset=$offset,
    name=$name
}"""

}
