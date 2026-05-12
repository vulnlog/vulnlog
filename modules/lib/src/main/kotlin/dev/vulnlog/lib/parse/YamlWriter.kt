// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.parse.v1.V1Mapper
import tools.jackson.databind.ObjectMapper

object YamlWriter {
    fun write(
        file: VulnlogFile,
        mapper: ObjectMapper,
    ): String {
        val dto = V1Mapper.toDto(file)
        return mapper
            .writeValueAsString(dto)
            .replace("\nproject:\n", "\n\nproject:\n")
            .replace("\nreleases:", "\n\nreleases:")
            .replace("\nvulnerabilities:", "\n\nvulnerabilities:")
    }
}
