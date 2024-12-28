package dev.vulnlog.dsl3

interface VlDslReleases {
    fun releases(block: VlReleaseContext.() -> Unit)
}
