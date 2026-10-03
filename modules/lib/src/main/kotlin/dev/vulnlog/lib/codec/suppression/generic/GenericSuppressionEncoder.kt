// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.generic

import dev.vulnlog.lib.model.suppression.SuppressionList
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

internal object GenericSuppressionEncoder {
    private val mapper =
        JsonMapper
            .builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .addModule(kotlinModule())
            .build()

    fun encode(list: SuppressionList): String {
        val dto = GenericMapper.toDto(list)
        return mapper.writeValueAsString(dto)
    }
}
