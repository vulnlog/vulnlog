package io.vulnlog.dsl2

interface VlFixActionTargetDependencyBuilder {
    /**
     * Specify the new dependency that should replace the previous vulnerable dependency.
     */
    fun with(dependencyName: String): VlFixAction
}
