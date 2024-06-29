package io.vulnlog.scriptdefinition

import io.vulnlog.dsl.VlVulnerabilitySetBlock
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

object VulnLogCompilationConfiguration : ScriptCompilationConfiguration({

    jvm {
        implicitReceivers(VlVulnerabilitySetBlock::class)
        dependenciesFromCurrentContext(wholeClasspath = true)
    }

    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
})
