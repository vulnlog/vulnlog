package ch.addere.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class SuppressionCommand : KoinComponent, CliktCommand(
    name = "supp",
    help =
        """
        Create suppression files based on vulnerability log entries.
        """.trimIndent(),
) {
    private val templates by option(
        "--templates",
        help =
            """
            Folder containing templates. One template per scanner expected.
            """.trimIndent(),
    ).file().required()

    private val commonOptions by CommonOptions()

    private val service by inject<Service>()

    override fun run() {
        val script: File = commonOptions.vulnFilePath
        val template: File = templates
        val suppressions = service.action(script, template)
        echo(
            suppressions.pretty(before = "\n\n", after = "\n\n", between = "\n\n", indentation = "\t")
                .joinToString(""),
        )
    }
}
