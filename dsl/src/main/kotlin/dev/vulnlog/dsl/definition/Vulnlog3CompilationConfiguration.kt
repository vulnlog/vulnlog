package dev.vulnlog.dsl.definition

import dev.vulnlog.dsl3.MyVuln
import dev.vulnlog.dsl3.VlDslReleases
import dev.vulnlog.dsl3.VlDslVuln
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

object Vulnlog3CompilationConfiguration : ScriptCompilationConfiguration(
    {
        implicitReceivers(VlDslReleases::class, VlDslVuln::class, MyVuln::class)

        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
            defaultImports("dev.vulnlog.dsl.*", "dev.vulnlog.dsl3.*")
        }

        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
    },
)
