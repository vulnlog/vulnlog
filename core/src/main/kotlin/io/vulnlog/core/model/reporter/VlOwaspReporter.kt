package io.vulnlog.core.model.reporter

import io.vulnlog.core.model.version.VlAffectedVersionSet

data class VlOwaspReporter(
    override val affectedVersionSet: VlAffectedVersionSet,
) : VlReporter {
    override val name = "Owasp Reporter"
}
