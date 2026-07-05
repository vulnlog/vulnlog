// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.result

import dev.vulnlog.lib.model.ParseValidationVersion
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.validation.FailureLocation
import dev.vulnlog.lib.model.validation.ParseFailure
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import org.snakeyaml.engine.v2.nodes.MappingNode

/** Result of checking that raw text is well-formed YAML declaring a schema version (step 1). */
sealed interface YamlParseResult {
    data class Valid(
        val rootNode: MappingNode,
        val schemaVersion: SchemaVersion,
    ) : YamlParseResult

    data class Invalid(
        val errorMessage: String,
        val location: FailureLocation? = null,
    ) : YamlParseResult
}

/** Result of mapping a valid YAML document onto the versioned DTO (step 2b, first half). */
sealed interface YamlParseDtoResult {
    data class Valid(
        val dto: VulnlogFileV1Dto,
        val schemaVersion: SchemaVersion,
        val validationVersion: ParseValidationVersion,
    ) : YamlParseDtoResult

    data class Invalid(
        val message: String,
        val location: FailureLocation? = null,
    ) : YamlParseDtoResult
}

/** Result of mapping the DTO onto the domain model (step 2b, second half). */
sealed interface DomainMappingResult {
    data class Valid(
        val file: VulnlogFile,
    ) : DomainMappingResult

    /** Every value without a domain representation, each naming its entry path. */
    data class Invalid(
        val failures: List<ParseFailure>,
    ) : DomainMappingResult
}
