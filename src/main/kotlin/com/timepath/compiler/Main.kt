import com.timepath.quakec.Logging
import java.util.Date
import com.timepath.quakec.compiler.CompilerOptions
import com.timepath.quakec.compiler.Compiler
import java.io.File
import com.timepath.quakec.vm.util.ProgramDataWriter
import com.timepath.quakec.vm.util.IOWrapper
import org.anarres.cpp.Feature
import com.timepath.quakec.QCC
import com.timepath.quakec.vm.ProgramData

val logger = Logging.new()

fun time(name: String, action: () -> Unit) {
    val start = Date()
    action()
    logger.info("$name: ${(Date().getTime() - start.getTime()).toDouble() / 1000} seconds")
}

fun main(args: Array<String>) {
    val opts = CompilerOptions()
    val xonotic = "${System.getProperties()["user.home"]}/IdeaProjects/xonotic"
    time("Total time") {
        [data] class Project(val root: String, val define: String, val out: String)

        val defs = listOf(
                Project("menu", "MENUQC", "menuprogs.dat"),
                Project("client", "CSQC", "csprogs.dat"),
                Project("server", "SVQC", "progs.dat")
        )
        defs.forEach { project ->
            time("Project time") {
                val compiler = Compiler(QCC, opts)
                        .includeFrom(File("$xonotic/data/xonotic-data.pk3dir/qcsrc/${project.root}/progs.src"))
                        .define(project.define)
                val compiled = compiler.compile()
                ProgramDataWriter(IOWrapper.File(File("out", project.out), write = true)).write(compiled as ProgramData)
            }
        }
    }
    time("GMQCC tests") {
        val gmqcc = Compiler(QCC, opts)
                .define("GMQCC")
                .define("__STD_GMQCC__")
        gmqcc.preprocessor.addFeatures(
                Feature.DIGRAPHS,
                Feature.TRIGRAPHS
        )
        val include = {(filter: (file: File) -> Boolean) ->
            val files = File("$xonotic/gmqcc/tests").listFiles(filter)
            if (files != null) {
                files.sort()
                files.forEach {
                    gmqcc.include(it)
                }
            }
        }
        include { it.name.endsWith(".qh") }
        //        include { it.name.endsWith(".qc") }
        gmqcc.include(File("$xonotic/gmqcc/tests/fieldparams.qc"))
        gmqcc.compile()
    }
}
