package com.timepath.quakec.vm.defs

import com.timepath.quakec.vm.ProgramDataReader
import com.timepath.quakec.vm.ProgramDataWriter
import spock.lang.Specification

class ProgramDataTest extends Specification {
    def "Load/save"() {
        when:
        def data = "${System.properties["user.home"]}/IdeaProjects/xonotic/gmqcc"
        def input = "${data}/progs.dat" as File
        def output = "${data}/progs2.dat" as File
        def reader = new ProgramDataReader(input)
        def writer = new ProgramDataWriter(output)
        writer.write(reader.read())
        then:
        input.bytes == output.bytes
    }
}
