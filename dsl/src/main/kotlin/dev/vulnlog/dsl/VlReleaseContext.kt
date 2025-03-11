package dev.vulnlog.dsl

interface VlReleaseContext {
    /**
     * Define a release with an optional publication date.
     *
     * @since v0.5.0
     */
    fun release(
        version: String,
        publicationDate: String? = null,
    )

    /**
     * Define a release branch with an optional publication date.
     *
     * @since v0.5.0
     */
    fun branch(
        name: String,
        block: (VlBranchContext.() -> Unit)? = null,
    )
}
