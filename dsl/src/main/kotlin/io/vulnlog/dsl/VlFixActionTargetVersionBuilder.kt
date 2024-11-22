package io.vulnlog.dsl

interface VlFixActionTargetVersionBuilder {
    /**
     * Specify the dependency version that fixes the reported vulnerable dependency.
     */
    fun toOrHigher(version: String)
}
