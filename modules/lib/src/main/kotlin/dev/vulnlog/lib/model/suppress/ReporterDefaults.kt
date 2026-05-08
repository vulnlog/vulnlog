// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.model.suppress

import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
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

        data object CargoAudit : Suppressable {
            override val vulnIdTypes: Set<KClass<out VulnId>>
                get() = setOf(VulnId.RustSec::class)
        }
    }
}
