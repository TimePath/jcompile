package com.timepath.quakec.vm.defs

import org.jetbrains.spek.api.Spek
import com.timepath.quakec.vm.util.ProgramDataReader
import com.timepath.quakec.vm.util.ProgramDataWriter
import java.io.File
import java.util.Arrays
import kotlin.test.assertTrue

class ProgramDataSpecs : Spek() {{

    val dir = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic/gmqcc"
    val input = File(dir, "progs.dat")
    if (input.exists()) {
        given("Progs data") {
            val data = ProgramDataReader(input).read()
            on("save") {
                val output = File(dir, "progs2.dat")
                val writer = ProgramDataWriter(output)
                writer.write(data)
                it("should be identical") {
                    val a = input.readBytes()
                    val b = output.readBytes()
                    assertTrue(Arrays.equals(a, b))
                }
            }
        }
    }
}
}
