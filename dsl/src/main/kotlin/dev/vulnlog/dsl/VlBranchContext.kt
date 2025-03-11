package dev.vulnlog.dsl

interface VlBranchContext {
    /**
     * Define a release with an optional publication date.
     *
     * @since v0.5.0
     */
    fun release(
        version: String,
        publicationDate: String? = null,
    )
}
