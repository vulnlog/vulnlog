// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

/**
 * Binds DTOs to and from an in-memory tree, honouring `@JsonProperty` renames, `@JsonInclude`
 * omission and `@JsonFormat` date formatting. It never reads or writes YAML: snakeyaml-engine parses
 * (see [dev.vulnlog.lib.parse.validation.parseToNodeTree]) and emits (see [CanonicalYaml]).
 */
internal val dtoMapper: ObjectMapper by lazy {
    JsonMapper
        .builder()
        .addModule(kotlinModule())
        // Unknown properties usually mean a newer schema than this binary.
        // silently dropping them would corrupt canonical rewrites, parsing fails instead.
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
}
