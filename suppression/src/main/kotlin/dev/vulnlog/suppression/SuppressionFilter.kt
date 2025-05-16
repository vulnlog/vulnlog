package dev.vulnlog.suppression

import dev.vulnlog.dsl.VlReporterImpl

class SuppressionFilter {
    fun filter(vulnsToSuppress: Set<VulnsPerBranchAndRecord>): Set<VulnsPerBranchAndRecord> {
        return vulnsToSuppress
            .map { entry ->
                val idMatcher = (entry.reporter as VlReporterImpl).config?.idMatcher!!
                val filtered = entry.vuln.filter { it.id.startsWith(idMatcher) }.toSet()
                entry.copy(vuln = filtered)
            }.toSet()
    }
}
