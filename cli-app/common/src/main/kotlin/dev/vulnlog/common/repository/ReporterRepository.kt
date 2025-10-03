package dev.vulnlog.common.repository

import dev.vulnlog.common.model.ReporterConfig
import dev.vulnlog.dsl.ReporterData

interface ReporterRepository {
    /**
     * Adds a reporter to the repository.
     *
     * @param reporter The reporter to be added, containing relevant data such as name.
     */
    fun add(reporter: ReporterData)

    /**
     * Adds a list of reporters to the repository.
     *
     * @param reporters A list of reporter objects to be added. Each reporter contains necessary data such as a name.
     */
    fun addAll(reporters: List<ReporterData>)

    fun getReportersWithConfig(): List<ReporterConfig>
}
