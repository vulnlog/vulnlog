package dev.vulnlog.suppression

import dev.vulnlog.common.model.ReporterConfig
import dev.vulnlog.common.repository.ReporterRepository

class SuppressionFilter(private val reporterRepository: ReporterRepository) {
    fun filter(vulnsToSuppress: Set<VulnsPerBranchAndRecord>): Set<VulnsPerBranchAndRecord> {
        val reporterConfigs = reporterRepository.getReportersWithConfig()
        return vulnsToSuppress
            .filter { entry ->
                val reporterConfig: ReporterConfig? =
                    reporterConfigs.firstOrNull { reporterConfig ->
                        reporterConfig.reporterName == entry.reporter
                    }
                reporterConfig != null
            }
            .map { entry ->
                val reporterConfig: ReporterConfig =
                    reporterConfigs.first { reporterConfig ->
                        reporterConfig.reporterName == entry.reporter
                    }
                val filter: Regex = reporterConfig.idMatcher.toRegex()
                val filteredSuppressVulnerabilities: Set<SuppressVulnerability> =
                    entry.vuln
                        .filter { filter.containsMatchIn(it.id.identifier) }
                        .toSet()
                entry.copy(vuln = filteredSuppressVulnerabilities)
            }
            .toSet()
    }
}
