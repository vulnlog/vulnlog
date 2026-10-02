// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

import dev.vulnlog.lib.app.ValidationOutcome
import dev.vulnlog.lib.app.parseDocument
import dev.vulnlog.lib.app.validateDocument
import dev.vulnlog.lib.document.validation.ParsedVulnlogProject
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import io.kotest.matchers.types.shouldBeInstanceOf

private const val TEST_FILE_NAME = "test.vl.yaml"

/** Runs [content] through the real parse stages, the way the layout commands see it. */
internal fun parsed(content: String): ParsedVulnlogProject =
    parseDocument(InputDocument(content, TEST_FILE_NAME))
        .shouldBeInstanceOf<ValidationOutcome.Ok<ParsedVulnlogProject>>()
        .project

/** Runs [content] through the domain stages too, the way the vulnerability commands see it. */
internal fun validated(content: String): ValidVulnlogProject =
    validateDocument(InputDocument(content, TEST_FILE_NAME))
        .shouldBeInstanceOf<ValidationOutcome.Ok<ValidVulnlogProject>>()
        .project
