package dev.vulnlog.dsl

interface VlRelease {
    /**
     * Create a product version without a release date.
     *
     * @param version describes the version string in semantic versioning format.
     * @return a not yet released product version.
     */
    fun release(version: String): VlReleaseValue

    /**
     * Create a product version with a release date.
     *
     * @param version describes the version string in semantic versioning format.
     * @param publicationDate use the format YYYY-MM-dd to specify.
     * @return a released product version.
     *
     */
    fun release(
        version: String,
        publicationDate: String,
    ): VlReleasePublishedValue
}
