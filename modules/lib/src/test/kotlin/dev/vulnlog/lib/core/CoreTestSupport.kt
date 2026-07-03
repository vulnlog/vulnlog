// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.parseVulnlogFile
import dev.vulnlog.lib.result.ParseResult
import io.kotest.matchers.types.shouldBeInstanceOf

/** Runs [content] through the real parse pipeline and expects it to succeed. */
internal fun parsed(content: String): ParseResult.Ok =
    parseVulnlogFile(createYamlMapper(), VulnlogFileRaw(content)).shouldBeInstanceOf<ParseResult.Ok>()

internal fun parsed(raw: VulnlogFileRaw): ParseResult.Ok = parsed(raw.content)
