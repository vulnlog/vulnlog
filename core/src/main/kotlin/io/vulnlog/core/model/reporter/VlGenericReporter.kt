package io.vulnlog.core.model.reporter

import io.vulnlog.core.model.version.VlAffectedVersionSet

data class VlGenericReporter(
    override val affectedVersionSet: VlAffectedVersionSet,
) : VlReporter {
    override val name: String = "Generic Reporter"
}
