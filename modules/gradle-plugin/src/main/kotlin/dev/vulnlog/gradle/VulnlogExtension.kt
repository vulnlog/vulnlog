// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

abstract class VulnlogExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val files: ConfigurableFileCollection = objects.fileCollection()

        val validate: VulnlogValidateExtension =
            objects.newInstance(VulnlogValidateExtension::class.java).apply {
                strict.convention(false)
            }

        val suppress: VulnlogSuppressExtension =
            objects.newInstance(VulnlogSuppressExtension::class.java)

        val report: VulnlogReportExtension =
            objects.newInstance(VulnlogReportExtension::class.java)

        fun validate(action: Action<VulnlogValidateExtension>) = action.execute(validate)

        fun suppress(action: Action<VulnlogSuppressExtension>) = action.execute(suppress)

        fun report(action: Action<VulnlogReportExtension>) = action.execute(report)
    }

interface VulnlogValidateExtension {
    val strict: Property<Boolean>
}

interface VulnlogSuppressExtension {
    val outputDir: DirectoryProperty
    val reporter: Property<String>
    val release: Property<String>
    val tags: SetProperty<String>
}

interface VulnlogReportExtension {
    val outputFile: RegularFileProperty
    val reporter: Property<String>
    val release: Property<String>
    val tags: SetProperty<String>
}
