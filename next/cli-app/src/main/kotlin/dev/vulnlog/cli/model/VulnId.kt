package dev.vulnlog.cli.model

sealed interface VulnId {
    val id: String

    data class Cve(override val id: String) : VulnId

    data class Ghsa(override val id: String) : VulnId

    data class RustSec(override val id: String) : VulnId

    data class Snyk(override val id: String) : VulnId
}
