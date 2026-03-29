package dev.vulnlog.cli.model.suppress

import dev.vulnlog.cli.model.VulnId
import kotlin.reflect.KClass

sealed interface Suppression

data object NotSuppressable : Suppression

sealed interface Suppressable : Suppression {
    val vulnIdTypes: Set<KClass<out VulnId>>

    data object Trivy : Suppressable {
        override val vulnIdTypes: Set<KClass<out VulnId>>
            get() = setOf(VulnId.Cve::class, VulnId.Ghsa::class)
    }
}
