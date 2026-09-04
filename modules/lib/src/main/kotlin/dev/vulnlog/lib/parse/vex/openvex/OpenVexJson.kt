// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.core.util.DefaultIndenter
import tools.jackson.core.util.DefaultPrettyPrinter
import tools.jackson.core.util.Separators
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

/** Two spaces and a plain "\n", never the platform line separator, so every platform writes the same bytes. */
private val indenter = DefaultIndenter("  ", "\n")

private val prettyPrinter =
    DefaultPrettyPrinter()
        .withObjectIndenter(indenter)
        .withArrayIndenter(indenter)
        .withSeparators(Separators.createDefaultInstance().withObjectNameValueSpacing(Separators.Spacing.AFTER))

/**
 * Binds the OpenVEX DTOs and parses baseline documents to a tree. Shared by [OpenVexWriter] and [OpenVexReader] so a
 * document this binary wrote and one it reads back go through the same configuration. An absent optional field is
 * left out rather than written as null.
 */
internal val openVexJson: ObjectMapper by lazy {
    JsonMapper
        .builder()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .defaultPrettyPrinter(prettyPrinter)
        .changeDefaultPropertyInclusion { inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL) }
        .addModule(kotlinModule())
        .build()
}
