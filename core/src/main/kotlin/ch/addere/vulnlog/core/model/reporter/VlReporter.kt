package ch.addere.vulnlog.core.model.reporter

import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet

interface VlReporter {
    val name: String
    val affectedVersionSet: VlAffectedVersionSet
}
