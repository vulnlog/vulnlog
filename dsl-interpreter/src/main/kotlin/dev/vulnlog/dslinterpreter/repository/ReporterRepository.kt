package dev.vulnlog.dslinterpreter.repository

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
}

class ReporterRepositoryImpl : ReporterRepository {
    private val reporterRepository = mutableListOf<ReporterData>()

    override fun add(reporter: ReporterData) {
        reporterRepository.add(reporter)
    }

    override fun addAll(reporters: List<ReporterData>) {
        reporterRepository.addAll(reporters)
    }
}
