// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.VulnlogFile

/**
 * Initializes a new VulnlogFile instance with the specified schema version, organization, project, and author.
 * The generated file includes an empty list of releases and vulnerabilities.
 *
 * @param schemaVersion The version of the schema to use for the Vulnlog file.
 * @param organization The organization name associated with the Vulnlog project.
 * @param project The project name associated with the Vulnlog project.
 * @param author The author of the Vulnlog project.
 * @return A new instance of VulnlogFile initialized with the specified parameters.
 */
fun init(
    schemaVersion: SchemaVersion,
    organization: String,
    project: String,
    author: String,
): VulnlogFile =
    VulnlogFile(
        schemaVersion = schemaVersion,
        project = Project(organization, project, author),
        releases = emptyList(),
        vulnerabilities = emptyList(),
    )
