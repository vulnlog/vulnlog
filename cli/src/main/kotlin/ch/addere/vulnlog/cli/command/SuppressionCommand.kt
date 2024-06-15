package ch.addere.vulnlog.cli.command

import ch.addere.vulnlog.cli.output.OutputService
import ch.addere.vulnlog.cli.suppressions.SuppressionComposition
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import java.io.File

class SuppressionCommand : KoinComponent, CliktCommand(
    name = "supp",
    help =
        """
        Create suppression files based on vulnerability log entries.
        """.trimIndent(),
) {
    private val templateDir by option(
        "--templates",
        help =
            """
            Folder containing templates. One template per scanner expected.
            """.trimIndent(),
    ).path().required()

    private val outputDir by option(
        "--outputs",
        help =
            """
            Folder containing templates. One template per scanner expected.
            """.trimIndent(),
    ).path()

    private val commonOptions by CommonOptions()

    private val service: Service by inject<Service>()

    override fun run() {
        val script: File = commonOptions.vulnFilePath
        val suppressions: List<SuppressionComposition> = service.action(script, templateDir)
        val outputService: OutputService = getOutputService()
        outputService.write(suppressions)
    }

    private fun getOutputService() =
        if (outputDir != null) {
            val a: OutputService by inject(qualifier = named("file")) { parametersOf(outputDir) }
            a
        } else {
            val echoFunction: (Any?, Boolean, Boolean) -> Unit = ::echo
            val a: OutputService by inject(qualifier = named("console")) { parametersOf(echoFunction) }
            a
        }
}
