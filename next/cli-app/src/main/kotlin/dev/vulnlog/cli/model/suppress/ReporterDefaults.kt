package dev.vulnlog.cli.model.suppress

import dev.vulnlog.cli.model.ReporterType
import dev.vulnlog.cli.model.VulnId
import kotlin.reflect.KClass

sealed interface Suppression

data object NotSuppressable : Suppression

sealed interface Suppressable : Suppression {
    val vulnIdTypes: Set<KClass<out VulnId>>

    sealed interface GenericFormat : Suppressable {
        data class Generic(
            val reporter: ReporterType,
        ) : Suppressable {
            override val vulnIdTypes: Set<KClass<out VulnId>>
                get() = setOf(VulnId.Cve::class, VulnId.Ghsa::class, VulnId.Snyk::class, VulnId.RustSec::class)
        }
    }

    sealed interface NativeFormat : Suppressable {
        data object Trivy : Suppressable {
            override val vulnIdTypes: Set<KClass<out VulnId>>
                get() = setOf(VulnId.Cve::class, VulnId.Ghsa::class)
        }

        data object Snyk : Suppressable {
            override val vulnIdTypes: Set<KClass<out VulnId>>
                get() = setOf(VulnId.Snyk::class)
        }
    }
}
