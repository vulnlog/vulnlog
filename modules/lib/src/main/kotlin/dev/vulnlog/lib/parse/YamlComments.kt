// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.VulnlogFileRaw
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Parse
import org.snakeyaml.engine.v2.comments.CommentType
import org.snakeyaml.engine.v2.events.CommentEvent

/**
 * True when [raw] contains YAML comments that a canonical rewrite drops. Blank lines and the
 * `# $schema:` header (which [YamlWriter] preserves) do not count as comments.
 */
fun hasYamlComments(raw: VulnlogFileRaw): Boolean {
    val settings = LoadSettings.builder().setParseComments(true).build()
    return Parse(settings).parseString(raw.content).any { event ->
        event is CommentEvent &&
            event.commentType != CommentType.BLANK_LINE &&
            !event.value.trim().startsWith("\$schema:")
    }
}

/**
 * True when [raw] carries the optional `# $schema:` header comment. The header only enables JSON
 * Schema support in editors; [YamlWriter] re-emits it for documents that already have it and omits
 * it for those that do not, so a write never introduces it on its own.
 */
fun hasSchemaHeader(raw: VulnlogFileRaw): Boolean {
    val settings = LoadSettings.builder().setParseComments(true).build()
    return Parse(settings).parseString(raw.content).any { event ->
        event is CommentEvent && event.value.trim().startsWith("\$schema:")
    }
}
