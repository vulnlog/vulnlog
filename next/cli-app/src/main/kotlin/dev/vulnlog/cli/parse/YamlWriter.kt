package dev.vulnlog.cli.parse

import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.parse.v1.V1Mapper
import tools.jackson.databind.ObjectMapper

object YamlWriter {
    fun write(
        file: VulnlogFile,
        mapper: ObjectMapper,
    ): String {
        val dto = V1Mapper.toDto(file)
        return mapper.writeValueAsString(dto)
            .replace("\nproject:\n", "\n\nproject:\n")
            .replace("\nreleases:", "\n\nreleases:")
            .replace("\nvulnerabilities:", "\n\nvulnerabilities:")
    }
}
