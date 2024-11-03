package io.vulnlog.dsl2

interface VlToFixActionOverwrite {
    /**
     * Update the specified dependency.
     */
    fun update(dependency: String): VlFixActionTargetVersionBuilder

    /**
     * Remove the specified dependency.
     */
    fun remove(dependency: String): VlOverwriteBuilder

    /**
     * Replace the specified dependency.
     */
    fun replace(dependency: String): VlOverwriteBuilder
}
