package ch.addere.vulnlog.core.model.reporter

import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet

data class VlOwaspReporter(override val affectedVersionSet: VlAffectedVersionSet) : VlReporter {
    override val name = "Owasp Reporter"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VlOwaspReporter

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
