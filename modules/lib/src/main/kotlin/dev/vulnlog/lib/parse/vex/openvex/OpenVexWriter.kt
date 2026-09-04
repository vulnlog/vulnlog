// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import tools.jackson.core.util.DefaultIndenter
import tools.jackson.core.util.DefaultPrettyPrinter
import tools.jackson.core.util.Separators
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

object OpenVexWriter {
    // Objects and arrays are indented with two spaces and a plain "\n", never the platform line separator, so the same document writes the same bytes everywhere.
    private val indenter = DefaultIndenter("  ", "\n")

    private val prettyPrinter =
        DefaultPrettyPrinter()
            .withObjectIndenter(indenter)
            .withArrayIndenter(indenter)
            .withSeparators(Separators.createDefaultInstance().withObjectNameValueSpacing(Separators.Spacing.AFTER))

    private val mapper =
        JsonMapper
            .builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .defaultPrettyPrinter(prettyPrinter)
            .addModule(kotlinModule())
            .build()

    fun write(document: OpenVexDocument): String = mapper.writeValueAsString(OpenVexMapper.toDto(document)) + "\n"
}
