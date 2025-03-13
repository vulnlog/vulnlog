package dev.vulnlog.dsl

import dev.vulnlog.dsl.util.toCamelCase
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

public data class ReleaseBranch(public val name: String) : Comparable<ReleaseBranch> {
    override fun compareTo(other: ReleaseBranch): Int {
        return ReleaseBranchProvider.compare(this, other)
    }

    /**
     * Returns a providable representation of the release branch name.
     */
    public fun providerName(): String {
        return name.toCamelCase()
    }
}

/**
 * A provider that can provide release branches.
 */
public interface ReleaseBranchProvider {
    public companion object Factory : ReadOnlyProperty<Any?, ReleaseBranch> {
        private val allReleases: MutableList<Rb> = mutableListOf()
        private var counter = 0

        public fun create(name: String): ReleaseBranch {
            val releaseBranch = ReleaseBranch(name)
            val rB = Rb(releaseBranch.providerName(), counter++, releaseBranch)
            allReleases.add(rB)
            return releaseBranch
        }

        override fun getValue(
            thisRef: Any?,
            property: KProperty<*>,
        ): ReleaseBranch {
            return allReleases.filter { it.providerName == property.name }.map { it.releaseBranch }.first()
        }

        public fun compare(
            a: ReleaseBranch,
            b: ReleaseBranch,
        ): Int {
            val rb1 = allReleases.first { it.releaseBranch == a }
            val rb2 = allReleases.first { it.releaseBranch == b }
            return rb1.position.compareTo(rb2.position)
        }

        public fun allReleases(): List<ReleaseBranch> = allReleases.map(Rb::releaseBranch)
    }
}

internal data class Rb(val providerName: String, val position: Int, val releaseBranch: ReleaseBranch)
