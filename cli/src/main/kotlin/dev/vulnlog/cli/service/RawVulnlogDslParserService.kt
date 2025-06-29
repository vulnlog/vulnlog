package dev.vulnlog.cli.service

import dev.vulnlog.dslinterpreter.ScriptingHost
import dev.vulnlog.dslinterpreter.impl.VlDslRootImpl
import java.io.File

class RawVulnlogDslParserService(private val host: ScriptingHost) {
    /**
     * Reads and parses a Vulnlog definition file along with associated script files in the same directory.
     *
     * @param vulnlogDefinition The main Vulnlog script file to be processed. Additional script files in the same
     * directory that end with "vl.kts" and have a different name are also processed.
     * @return The parsed Vulnlog DSL root implementation object representing the script evaluation result.
     */
    fun readAndParse(vulnlogDefinition: File): VlDslRootImpl {
        val files =
            vulnlogDefinition.parentFile
                .listFiles { file -> file.name.endsWith("vl.kts") && file.name != vulnlogDefinition.name }
                ?.toList() ?: emptyList()
        val defFirst: List<File> = listOf(vulnlogDefinition).plus(files)

        val result = host.eval(defFirst)
        result.onFailure { error(it) }

        return result.getOrThrow() as VlDslRootImpl
    }
}
