// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.suppression

import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import kotlin.reflect.KClass

sealed interface SuppressionFormat {
    val vulnIdTypes: Set<KClass<out VulnId>>

    data class Generic(
        val reporter: ReporterType,
    ) : SuppressionFormat {
        override val vulnIdTypes: Set<KClass<out VulnId>>
            get() = setOf(VulnId.Cve::class, VulnId.Ghsa::class, VulnId.Snyk::class, VulnId.RustSec::class)
    }

    data object Trivy : SuppressionFormat {
        override val vulnIdTypes: Set<KClass<out VulnId>>
            get() = setOf(VulnId.Cve::class, VulnId.Ghsa::class)
    }

    data object Snyk : SuppressionFormat {
        override val vulnIdTypes: Set<KClass<out VulnId>>
            get() = setOf(VulnId.Snyk::class)
    }

    data object CargoAudit : SuppressionFormat {
        override val vulnIdTypes: Set<KClass<out VulnId>>
            get() = setOf(VulnId.RustSec::class)
    }
}
