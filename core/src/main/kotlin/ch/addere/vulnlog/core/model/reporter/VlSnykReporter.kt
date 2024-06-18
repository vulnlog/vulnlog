package ch.addere.vulnlog.core.model.reporter

import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet

data class VlSnykReporter(
    val snykId: String,
    override val affectedVersionSet: VlAffectedVersionSet,
    val filters: Set<String> = emptySet(),
) : VlReporter {
    override val name: String = "Snyk Reporter"
}
