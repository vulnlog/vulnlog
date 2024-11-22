package dev.vulnlog.dsl

interface VlToFixAction {
    /**
     * Update the specified dependency.
     */
    fun update(dependency: String): VlFixActionTargetVersionBuilder

    /**
     * Remove the specified dependency.
     */
    fun remove(dependency: String)

    /**
     * Replace the specified dependency.
     */
    fun replace(dependency: String): VlFixActionTargetDependencyBuilder
}
