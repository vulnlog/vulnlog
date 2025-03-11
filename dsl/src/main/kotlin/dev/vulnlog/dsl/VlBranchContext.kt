package dev.vulnlog.dsl

public interface VlBranchContext {
    /**
     * Define a release with an optional publication date.
     *
     * @since v0.5.0
     */
    public fun release(
        version: String,
        publicationDate: String? = null,
    )
}
