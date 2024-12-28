package dev.vulnlog.dsl3

interface VlBranchContext {
    fun release(
        version: String,
        publicationDate: String? = null,
    )
}
