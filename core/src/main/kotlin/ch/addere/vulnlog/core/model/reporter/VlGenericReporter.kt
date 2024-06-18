package ch.addere.vulnlog.core.model.reporter

import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet

data class VlGenericReporter(
    override val affectedVersionSet: VlAffectedVersionSet,
) : VlReporter {
    override val name: String = "Generic Reporter"
}
