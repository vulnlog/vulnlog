package dev.vulnlog.dsl

import java.time.LocalDate
import kotlin.reflect.KProperty

/**
 * A provider that can provide release branches.
 */
public class ReleaseBranch private constructor(
    private val id: Int,
    public val name: String,
) : Comparable<ReleaseBranch> {
    public companion object Factory {
        private var counter = 0
        public val allReleases: MutableList<ReleaseBranch> = mutableListOf()

        public fun create(name: String): ReleaseBranch {
            val releaseBranch = ReleaseBranch(counter++, name)
            allReleases.add(releaseBranch)
            return releaseBranch
        }

        public operator fun getValue(
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

public sealed interface ReleaseBranchData {
    public val name: String
}

public data class ReleaseBranchDataImpl(override val name: String) : ReleaseBranchData

public data object DefaultReleaseBranchDataImpl : ReleaseBranchData {
    override val name: String = "Default Release Branch"
}

public sealed interface ReleaseVersionData {
    public val version: String
    public val releaseDate: LocalDate?
}

public data class ReleaseVersionDataImpl(override val version: String, override val releaseDate: LocalDate?) :
    ReleaseVersionData
