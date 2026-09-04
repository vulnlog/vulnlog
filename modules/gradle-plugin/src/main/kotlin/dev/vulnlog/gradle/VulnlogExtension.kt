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

        val vex: VulnlogVexExtension =
            objects.newInstance(VulnlogVexExtension::class.java)

        val fmt: VulnlogFmtExtension =
            objects.newInstance(VulnlogFmtExtension::class.java).apply {
                check.convention(false)
            }

        fun validate(action: Action<VulnlogValidateExtension>) = action.execute(validate)

        fun suppress(action: Action<VulnlogSuppressExtension>) = action.execute(suppress)

        fun report(action: Action<VulnlogReportExtension>) = action.execute(report)

        fun vex(action: Action<VulnlogVexExtension>) = action.execute(vex)

        fun fmt(action: Action<VulnlogFmtExtension>) = action.execute(fmt)
    }

interface VulnlogValidateExtension {
    val strict: Property<Boolean>
}

interface VulnlogFmtExtension {
    val check: Property<Boolean>
}

interface VulnlogSuppressExtension {
    val outputDir: DirectoryProperty
    val format: Property<String>
    val reporter: Property<String>
    val asOf: Property<String>
    val tags: SetProperty<String>
}

/** Groups the report types, so each report keeps its own settings under `report { }`. */
abstract class VulnlogReportExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val impact: VulnlogImpactReportExtension =
            objects.newInstance(VulnlogImpactReportExtension::class.java)

        val changelog: VulnlogChangelogReportExtension =
            objects.newInstance(VulnlogChangelogReportExtension::class.java)

        fun impact(action: Action<VulnlogImpactReportExtension>) = action.execute(impact)

        fun changelog(action: Action<VulnlogChangelogReportExtension>) = action.execute(changelog)
    }

interface VulnlogImpactReportExtension {
    val outputFile: RegularFileProperty
    val reporter: Property<String>
    val asOf: Property<String>
    val tags: SetProperty<String>
    val states: SetProperty<String>
    val verdicts: SetProperty<String>
    val dispositions: SetProperty<String>
}

interface VulnlogChangelogReportExtension {
    val outputFile: RegularFileProperty
    val format: Property<String>
    val brief: Property<Boolean>
    val fixedIn: Property<String>
    val reporter: Property<String>
    val asOf: Property<String>
    val tags: SetProperty<String>
}

abstract class VulnlogVexExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val openvex: VulnlogOpenVexExtension = objects.newInstance(VulnlogOpenVexExtension::class.java)

        fun openvex(action: Action<VulnlogOpenVexExtension>) = action.execute(openvex)
    }

interface VulnlogOpenVexExtension {
    val outputFile: RegularFileProperty
}
