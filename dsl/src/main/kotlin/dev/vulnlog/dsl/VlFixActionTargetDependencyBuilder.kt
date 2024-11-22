package dev.vulnlog.dsl

interface VlFixActionTargetDependencyBuilder {
    /**
     * Specify the new dependency that should replace the previous vulnerable dependency.
     */
    fun with(dependencyName: String)
}
