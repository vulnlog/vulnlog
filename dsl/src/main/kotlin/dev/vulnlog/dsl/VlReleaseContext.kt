package dev.vulnlog.dsl

interface VlReleaseContext {
    fun release(
        version: String,
        publicationDate: String? = null,
    )

    fun branch(
        name: String,
        block: (VlBranchContext.() -> Unit)? = null,
    )
}
