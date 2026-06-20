// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.suppress

import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import kotlin.reflect.KClass

sealed interface SuppressionFormat {
    val vulnIdTypes: Set<KClass<out VulnId>>

    sealed interface GenericFormat : SuppressionFormat {
        data class Generic(
            val reporter: ReporterType,
        ) : GenericFormat {
            override val vulnIdTypes: Set<KClass<out VulnId>>
                get() = setOf(VulnId.Cve::class, VulnId.Ghsa::class, VulnId.Snyk::class, VulnId.RustSec::class)
        }
    }

    sealed interface NativeFormat : SuppressionFormat {
        data object Trivy : NativeFormat {
            override val vulnIdTypes: Set<KClass<out VulnId>>
                get() = setOf(VulnId.Cve::class, VulnId.Ghsa::class)
        }

        data object Snyk : NativeFormat {
            override val vulnIdTypes: Set<KClass<out VulnId>>
                get() = setOf(VulnId.Snyk::class)
        }

        data object CargoAudit : NativeFormat {
            override val vulnIdTypes: Set<KClass<out VulnId>>
                get() = setOf(VulnId.RustSec::class)
        }
    }
}
