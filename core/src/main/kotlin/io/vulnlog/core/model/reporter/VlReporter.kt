package io.vulnlog.core.model.reporter

import io.vulnlog.core.model.version.VlAffectedVersionSet

interface VlReporter {
    val name: String
    val affectedVersionSet: VlAffectedVersionSet
}
