package dev.vulnlog.dsl2

import java.time.LocalDate

interface VlReleaseBranch {
    val name: String

    val lifeCyclePhases: List<VlLifeCyclePhase>

    val releases: List<MyRelease>

    val predecessor: VlReleaseBranch?

    fun release(version: String): MyRelease?

    fun releaseJustBefore(date: LocalDate): MyRelease

    fun releaseJustAfter(date: LocalDate): MyRelease

    fun supportLifetime(): List<Pair<String, VlPhasePeriod>> = emptyList()
}

object VlReleaseBranchDefault : VlReleaseBranch {
    override val name: String = "Default Branch"

    override val lifeCyclePhases: List<VlLifeCyclePhase> = emptyList()

    override val releases: List<MyRelease> = emptyList()

    override val predecessor: VlReleaseBranch = VlReleaseBranchDefault

    override fun release(version: String) = null

    override fun releaseJustBefore(date: LocalDate): MyRelease = DefaultMyRelease

    override fun releaseJustAfter(date: LocalDate): MyRelease = DefaultMyRelease
}

data class VlReleaseBranchImpl(
    override val name: String,
    override val lifeCyclePhases: List<VlLifeCyclePhase>,
    override val releases: List<MyRelease>,
    override val predecessor: VlReleaseBranch? = null,
) : VlReleaseBranch {
    override fun release(version: String) = releases.firstOrNull { it.version == VlVersionImpl(version) }

    /**
     * Returns the latest published release before [date].
     */
    override fun releaseJustBefore(date: LocalDate): MyRelease {
        return releases.windowed(size = 2, step = 1)
            .firstOrNull {
                val first = it.first().publication.publication
                val second = it.last().publication.publication

                date >= first && date < second
            }?.first() ?: releases.last()
    }

    /**
     * Return the next release after [date]. Release may or may not be published yet.
     */
    override fun releaseJustAfter(date: LocalDate): MyRelease {
        return releases.windowed(size = 2, step = 1)
            .filter {
                val first = it.first().publication.publication
                val second = it.last().publication.publication

                date >= first && date < second
            }.map { it.last() }.firstOrNull() ?: releases.last()
    }

    override fun supportLifetime(): List<Pair<String, VlPhasePeriod>> {
        val result = mutableListOf<Pair<String, VlPhasePeriod>>()
        var start = releases.first().publication.publication

        for (phase in lifeCyclePhases) {
            result += Pair(phase.name, VlPhasePeriod(start, start.plusMonths(phase.months)))
            start = start.plusMonths(phase.months).plusDays(1)
        }
        return result
    }
}
