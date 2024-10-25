package io.vulnlog.dsl2

interface VlToFixAction<out T> {
    /**
     * Update the specified dependency.
     */
    fun update(dependency: String): VlFixActionTargetVersionBuilder<T>

    /**
     * Remove the specified dependency.
     */
    fun remove(dependency: String): T

    /**
     * Replace the specified dependency.
     */
    fun replace(dependency: String): VlFixActionTargetDependencyBuilder<T>
}
