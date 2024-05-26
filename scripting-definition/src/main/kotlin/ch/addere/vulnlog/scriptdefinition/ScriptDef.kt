package ch.addere.vulnlog.scriptdefinition

import ch.addere.vulnlog.core.model.reporter.VlReporterSet
import ch.addere.vulnlog.core.model.version.VlReleaseGroup
import ch.addere.vulnlog.core.model.version.VlVersion
import ch.addere.vulnlog.core.model.vulnerability.VlVulnerability
import ch.addere.vulnlog.dsl2.VlVulnerabilityBlock
import ch.addere.vulnlog.dsl2.VlVulnerabilitySetBlock
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    displayName = "Vulnerability Log",
    fileExtension = "vulnlog.kts",
    compilationConfiguration = VulnLogCompilationConfiguration::class,
    evaluationConfiguration = VulnLogEvaluationConfiguration::class,
)
open class VulnLogScript {
    val allVersions = mutableSetOf<VlVersion>()
    val allGroups = mutableSetOf<VlReleaseGroup>()
    val allVulnerabilities = mutableSetOf<VlVulnerability>()

    fun version(version: String): VlVersion {
        val vlVersion = VlVersion(version)
        allVersions += vlVersion
        return vlVersion
    }

    fun branch(
        name: String,
        upcoming: VlVersion,
        vararg released: VlVersion,
    ): VlReleaseGroup {
        val vlReleaseGroup = VlReleaseGroup(name, upcoming, released.toList())
        allGroups += vlReleaseGroup
        return vlReleaseGroup
    }

    fun cve(
        id: String,
        init: VlVulnerabilityBlock.() -> Unit,
    ) = with(VlVulnerabilityBlock()) {
        init()
        allVulnerabilities +=
            VlVulnerability(
                id,
                VlReporterSet(reporters),
                ignoreVersions,
                suppressVersions,
                fixedInVersions,
            )
    }
}

object VulnLogCompilationConfiguration : ScriptCompilationConfiguration({

    jvm {
        implicitReceivers(VlVulnerabilitySetBlock::class)
        dependenciesFromCurrentContext(wholeClasspath = true)
    }

    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
})

object VulnLogEvaluationConfiguration : ScriptEvaluationConfiguration({

    jvm {
        implicitReceivers(VlVulnerabilitySetBlock())
    }
})
