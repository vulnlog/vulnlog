package ch.addere.vulnlog.core.model.reporter

import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet

data class VlSnykReporter(
    val snykId: String,
    override val affectedVersionSet: VlAffectedVersionSet,
    val filters: Set<String> = emptySet(),
) : VlReporter {
    override val name: String = "Snyk Reporter"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VlSnykReporter

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
