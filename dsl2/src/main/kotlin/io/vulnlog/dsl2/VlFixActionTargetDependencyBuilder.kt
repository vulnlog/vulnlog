package io.vulnlog.dsl2

interface VlFixActionTargetDependencyBuilder<out T> {
    /**
     * Specify the new dependency that should replace the previous vulnerable dependency.
     */
    fun with(dependencyName: String): T
}
