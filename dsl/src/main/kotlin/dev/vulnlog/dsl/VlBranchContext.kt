package dev.vulnlog.dsl

interface VlBranchContext {
    fun release(
        version: String,
        publicationDate: String? = null,
    )
}
