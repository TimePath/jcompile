package com.timepath.q1vm

import com.timepath.q1vm.util.ProgramDataReader
import com.timepath.q1vm.util.ProgramDataWriter
import java.io.File
import java.util.Arrays
import kotlin.test.assertTrue
import org.junit.Test as test

class ProgramDataTest {

    test fun `save should be identical`() {
        val dir = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic/gmqcc"
        val input = File(dir, "progs.dat")
        if (input.exists()) {
            val data = ProgramDataReader(input).read()
            val output = File(dir, "progs2.dat")
            val writer = ProgramDataWriter(output)
            writer.write(data)
            val a = input.readBytes()
            val b = output.readBytes()
            assertTrue(Arrays.equals(a, b))
        }
    }
}
