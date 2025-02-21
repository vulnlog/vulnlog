package dev.vulnlog.dsl

interface VlDslReleases {
    fun releases(block: VlReleaseContext.() -> Unit)
}
