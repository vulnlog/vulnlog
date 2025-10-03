package dev.vulnlog.dslinterpreter.repository

import dev.vulnlog.common.model.ReporterConfig
import dev.vulnlog.common.repository.ReporterRepository
import dev.vulnlog.dsl.ReporterData
import dev.vulnlog.dslinterpreter.impl.ReporterDataImpl

class ReporterRepositoryImpl : ReporterRepository {
    private val reporterRepository = mutableListOf<ReporterData>()

    override fun add(reporter: ReporterData) {
        reporterRepository.add(reporter)
    }

    override fun addAll(reporters: List<ReporterData>) {
        reporterRepository.addAll(reporters)
    }

    override fun getReportersWithConfig(): List<ReporterConfig> {
        return reporterRepository
            .filterIsInstance<ReporterDataImpl>()
            .filter { it.config != null }
            .map {
                ReporterConfig(
                    reporterName = it.name,
                    templateFilename = it.config!!.templateFilename,
                    idMatcher = it.config.idMatcher,
                    template = it.config.template,
                )
            }
    }
}
