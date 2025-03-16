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
    /**
     * Provide a release branche for the specified variable name.
     *
     * @since v0.5.0
     */
    public companion object Factory : ReadOnlyProperty<Any?, ReleaseBranch> {
        private val allReleases: MutableList<ProviderData<ReleaseBranch>> = mutableListOf()
        private var counter = 0

        public fun create(name: String): ReleaseBranch {
            val releaseBranch = ReleaseBranch(name)
            val providerData = ProviderData(releaseBranch.providerName(), counter++, releaseBranch)
            allReleases.add(providerData)
            return releaseBranch
        }

        override fun getValue(
            thisRef: Any?,
            property: KProperty<*>,
        ): ReleaseBranch {
            return allReleases.filter { it.providerName == property.name }.map { it.dataClass }.first()
        }

        public fun compare(
            a: ReleaseBranch,
            b: ReleaseBranch,
        ): Int {
            val aPosition = allReleases.first { it.dataClass == a }.position
            val bPosition = allReleases.first { it.dataClass == b }.position
            return aPosition.compareTo(bPosition)
        }

        public fun allReleases(): List<ReleaseBranch> = allReleases.map(ProviderData<ReleaseBranch>::dataClass)
    }
}

/**
 * A provider that can provide reporters.
 */
public interface ReporterProvider {
    /**
     * Provides a reporter for the specified variable name.
     *
     * @since v0.6.0
     */
    public companion object Factory : ReadOnlyProperty<Any?, VlReporter> {
        private val allReporters: MutableList<ProviderData<VlReporter>> = mutableListOf()
        private var counter = 0

        public fun create(name: String): VlReporter {
            val reporter: VlReporter = VlReporterImpl(name)
            val providerData = ProviderData(reporter.providerName(), counter++, reporter)
            allReporters.add(providerData)
            return reporter
        }

        override fun getValue(
            thisRef: Any?,
            property: KProperty<*>,
        ): VlReporter {
            return allReporters.filter { it.providerName == property.name }.map { it.dataClass }.first()
        }

        public fun allReleases(): List<VlReporter> = allReporters.map(ProviderData<VlReporter>::dataClass)
    }
}

internal data class ProviderData<T>(val providerName: String, val position: Int, val dataClass: T)
