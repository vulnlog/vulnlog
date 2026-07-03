// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule

fun createYamlMapper(): ObjectMapper =
    YAMLMapper
        .builder()
        .addModule(kotlinModule())
        // Unknown properties usually mean a newer schema than this binary; silently dropping them
        // would corrupt canonical rewrites, so parsing fails instead.
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
