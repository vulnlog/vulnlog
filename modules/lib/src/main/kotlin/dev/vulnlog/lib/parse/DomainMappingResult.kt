// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.ParseFailure
import dev.vulnlog.lib.parse.dto.DtoVersion
import dev.vulnlog.lib.parse.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.parse.mapper.DtoV1Mapper

/** Whether every value of a DTO has a domain representation. */
sealed interface DomainMappingResult {
    data class Rejected(
        val problems: List<ParseFailure>,
    ) : DomainMappingResult

    data class Mapped(
        val vulnlogProjectFile: VulnlogFile,
    ) : DomainMappingResult
}

/** Maps a DTO of any supported schema version onto the domain model. */
fun mapToDomain(dto: DtoVersion): DomainMappingResult =
    when (dto) {
        is VulnlogFileV1Dto -> DtoV1Mapper.toDomain(SchemaVersion.V1, dto)
    }
