package com.timepath.quakec.vm.defs

import com.timepath.quakec.vm.defs.Header.Section

data class Header(val version: Int,
                  val crc: Int,
                  val entityCount: Int,
                  val statements: Section,
                  val globalDefs: Section,
                  val fieldDefs: Section,
                  val functions: Section,
                  val stringData: Section,
                  val globalData: Section) {

    data class Section(val offset: Int, val count: Int)
}