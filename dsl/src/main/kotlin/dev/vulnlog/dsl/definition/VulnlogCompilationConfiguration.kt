package dev.vulnlog.dsl.definition

import dev.vulnlog.dsl.VlDslRoot
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

object VulnlogCompilationConfiguration : ScriptCompilationConfiguration(
    {
        implicitReceivers(VlDslRoot::class)

        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
            defaultImports("dev.vulnlog.dsl.*")
        }

        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
    },
)
