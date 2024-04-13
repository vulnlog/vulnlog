package ch.addere.scripting.definition

import ch.addere.dsl.ReleaseBranch
import ch.addere.dsl.ReleaseBranchBuilder
import ch.addere.dsl.SupportedBranches
import ch.addere.dsl.SupportedBranchesBuilder
import ch.addere.dsl.Version
import ch.addere.dsl.VulnLog
import ch.addere.dsl.VulnLogsBuilder
import ch.addere.dsl.Vulnerability
import ch.addere.dsl.VulnerabilityBuilder
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.isStandalone
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    displayName = "Vulnerability Log",
    fileExtension = "vulnlog.kts",
    compilationConfiguration = VulnLogCompilationConfiguration::class,
    evaluationConfiguration = VulnLogEvaluationConfiguration::class,
)
open class VulnLogScript : VulnLog {
    override val releaseBranch = mutableSetOf<ReleaseBranch>()
    override var branches: SupportedBranches? = null
    override val vulnerabilities = mutableSetOf<Vulnerability>()

    fun release(block: ReleaseBranchBuilder.() -> Unit) =
        with(ReleaseBranchBuilder()) {
            block()
            val rb = this.build()
            releaseBranch += rb
            rb
        }

    fun branches(block: SupportedBranchesBuilder.() -> Unit) =
        with(SupportedBranchesBuilder()) {
            block()
            branches = this.build()
        }

    fun vulnerability(block: VulnerabilityBuilder.() -> Unit) =
        with(VulnerabilityBuilder()) {
            block()
            vulnerabilities.add(this.build())
        }
}

object VulnLogCompilationConfiguration : ScriptCompilationConfiguration({

    defaultImports(Version::class)
    isStandalone(false)
    implicitReceivers(VulnLogsBuilder::class)

    jvm {
        implicitReceivers(VulnLogsBuilder::class)
        dependenciesFromCurrentContext(wholeClasspath = true)
    }

    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
}) {
    private fun readResolve(): Any = VulnLogCompilationConfiguration
}

object VulnLogEvaluationConfiguration : ScriptEvaluationConfiguration({

    jvm {
        implicitReceivers(VulnLogsBuilder())
    }
}) {
    private fun readResolve(): Any = VulnLogEvaluationConfiguration
}
