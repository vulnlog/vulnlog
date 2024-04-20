package ch.addere.cli.command

import ch.addere.cli.suppressions.OwaspDependencyCheckerSuppressor
import ch.addere.cli.suppressions.SnykSuppressor
import ch.addere.dsl.VulnLog
import ch.addere.scripting.host.ScriptingHost
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

class SuppressionCommand : CliktCommand(
    name = "supp",
    help =
        """
        Create suppression files based on vulnerability log entries.
        """.trimIndent(),
) {
    val templates by option(
        "--templates",
        help =
            """
            Folder containing templates. One template per scanner expected.
            """.trimIndent(),
    ).file().required()

    private val commonOptions by CommonOptions()

    override fun run() {
        val result: VulnLog = ScriptingHost().evalScript(commonOptions.vulnFilePath)
        val suppressions =
            if (templates.name.endsWith(".xml")) {
                val marker = "<vulnlog-marker/>"
                val suppressor = OwaspDependencyCheckerSuppressor(templates, marker)
                suppressor.createSuppressions(result.vulnerabilities)
            } else {
                val marker = "vulnlog-marker"
                val suppressor = SnykSuppressor(templates, marker)
                suppressor.createSuppressions(result.vulnerabilities)
            }

        echo(
            suppressions.pretty(before = "\n\n", after = "\n\n", between = "\n\n", indentation = "\t")
                .joinToString(""),
        )
    }
}
