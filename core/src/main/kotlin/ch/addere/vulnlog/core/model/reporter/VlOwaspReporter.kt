package ch.addere.vulnlog.core.model.reporter

import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet

data class VlOwaspReporter(
    override val affectedVersionSet: VlAffectedVersionSet,
) : VlReporter {
    override val name = "Owasp Reporter"
}
