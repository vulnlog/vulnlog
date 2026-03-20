package dev.vulnlog.cli.model

sealed interface VulnId {
    data class Cve(val id: String) : VulnId

    data class Ghsa(val id: String) : VulnId

    data class Rust(val id: String) : VulnId

    data class Snyk(val id: String) : VulnId
}
