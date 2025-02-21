package dev.vulnlog.dsl

import java.time.LocalDate
import kotlin.reflect.KProperty

class ReleaseBranch private constructor(private val id: Int, val name: String) : Comparable<ReleaseBranch> {
    companion object Factory {
        private var counter = 0
        val allReleases = mutableListOf<ReleaseBranch>()

        fun create(name: String): ReleaseBranch {
            val releaseBranch = ReleaseBranch(counter++, name)
            allReleases.add(releaseBranch)
            return releaseBranch
        }

        operator fun getValue(
            thisRef: Any?,
            property: KProperty<*>,
        ): ReleaseBranch {
            return allReleases.first { it.name == property.name }
        }
    }

    override fun compareTo(other: ReleaseBranch): Int {
        return id.compareTo(other.id)
    }

    override fun toString(): String {
        return "ReleaseBranch(name='$name')"
    }
}

sealed interface ReleaseBranchData {
    val name: String
}

data class ReleaseBranchDataImpl(override val name: String) : ReleaseBranchData

data object DefaultReleaseBranchDataImpl : ReleaseBranchData {
    override val name: String = "Default Release Branch"
}

sealed interface ReleaseVersionData {
    val version: String
    val releaseDate: LocalDate?
}

data class ReleaseVersionDataImpl(override val version: String, override val releaseDate: LocalDate?) :
    ReleaseVersionData
