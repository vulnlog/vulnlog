// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression.generic

import dev.vulnlog.lib.model.suppress.SuppressionOutput
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

object GenericSuppressionWriter {
    private val mapper =
        JsonMapper
            .builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .addModule(kotlinModule())
            .build()

    fun write(inputData: SuppressionOutput.GenericSuppression): String {
        val dto = GenericMapper.toDto(inputData)
        return mapper.writeValueAsString(dto)
    }
}
