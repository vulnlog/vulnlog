package io.vulnlog.cli.command

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

class CommonOptions : OptionGroup("Standard Options:") {
    val vulnFilePath by option(
        "-f",
        "--file",
        help =
            """
            Specify the vulnerability log file as input.
            """.trimIndent(),
    ).file().required()
}
