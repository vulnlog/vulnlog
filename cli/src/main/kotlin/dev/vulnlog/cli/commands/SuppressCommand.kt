package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.suppression.ConsoleWriter
import dev.vulnlog.suppression.FileWriter
import dev.vulnlog.suppression.OutputWriter
import dev.vulnlog.suppression.service.SuppressCommandArguments
import dev.vulnlog.suppression.service.SuppressService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class SuppressCommand : CliktCommand(), KoinComponent {
    override fun help(context: Context): String =
        """
        Generate a Vulnlog suppression files per release branch and reporter.
        
        The --branch and --vuln filters can be used to reduce vulnerabilities in the suppression files. Suppression 
        files for all branches are created, even if no suppressed vulnerabilities are found for a branch.
        """.trimIndent()

    private val templateDirHelpText =
        """
        Directory containing template files.
        Per reporter one template file is expected. The file name must correspond with the `templateFilename` in
        the Vulnlog DSL definitions file (`reporter.suppression.templateFilname`).
        """.trimIndent()
    private val templateDir: File by option("--template-dir")
        .file(mustExist = false, canBeDir = true, canBeFile = false)
        .required()
        .help(templateDirHelpText)
    private val suppressionOutputDirHelpText =
        """
        Directory to write suppression files. If not specified, suppression files are written to STDOUT. 
        The directory will be created if it does not exist.
        """.trimIndent()
    private val suppressionOutputDir: File? by option("--output")
        .file(mustExist = false, canBeDir = true, canBeFile = false)
        .help(suppressionOutputDirHelpText)

    private val data by requireObject<SubcommandData>()
    private val suppressService: SuppressService by inject()

    override fun run() {
        val outputWriter: OutputWriter = suppressionOutputDir?.let { FileWriter(it) } ?: ConsoleWriter(::echo)
        val config = SuppressCommandArguments(templateDir, outputWriter)
        suppressService.generateSuppression(config, data.vulnEntriesFiltered)
    }
}
