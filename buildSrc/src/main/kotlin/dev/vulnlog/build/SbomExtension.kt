// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.build

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface SbomExtension {
    val rootPurl: Property<String>
    val vexDocument: RegularFileProperty
    val containerPurl: Property<String>
    val containerVexDocument: RegularFileProperty
}
