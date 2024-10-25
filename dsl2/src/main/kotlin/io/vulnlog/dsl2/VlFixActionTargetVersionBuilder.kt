package io.vulnlog.dsl2

interface VlFixActionTargetVersionBuilder<out T> {
    /**
     * Specify the dependency version that fixes the reported vulnerable dependency.
     */
    fun toOrHigher(version: String): T
}
