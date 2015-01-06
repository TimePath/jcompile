package com.timepath.quakec.vm.defs

import org.jetbrains.spek.api.Spek
import com.timepath.quakec.vm.ProgramDataReader
import com.timepath.quakec.vm.ProgramDataWriter
import java.io.File
import java.util.Arrays
import kotlin.test.assertTrue

class ProgramDataSpecs : Spek() {{

    given("Progs data") {
        val dir = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic/gmqcc"
        val input = File("${dir}/progs.dat")
        val data = ProgramDataReader(input).read()
        on("save") {
            val output = File("${dir}/progs2.dat")
            val writer = ProgramDataWriter(output)
            writer.write(data)
            it("should be identical") {
                assertTrue(Arrays.equals(input.readBytes(), output.readBytes()))
            }
        }
    }

}
}