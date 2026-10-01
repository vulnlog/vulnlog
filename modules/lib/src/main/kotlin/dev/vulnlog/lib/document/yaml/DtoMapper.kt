// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.yaml

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

/** Jackson only binds DTOs to plain trees; snakeyaml-engine reads and writes the YAML itself. */
internal val dtoMapper: ObjectMapper by lazy {
    JsonMapper
        .builder()
        .addModule(kotlinModule())
        // Unknown properties usually mean a newer schema; dropping them would corrupt a canonical rewrite.
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
}
