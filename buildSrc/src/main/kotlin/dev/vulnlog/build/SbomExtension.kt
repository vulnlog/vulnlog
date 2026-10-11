// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.build

import org.gradle.api.provider.Property

interface SbomExtension {
    val rootPurl: Property<String>
    val containerPurl: Property<String>
}
