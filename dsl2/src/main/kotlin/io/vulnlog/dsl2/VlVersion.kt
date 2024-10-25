package io.vulnlog.dsl2

interface VlVersion {
    /**
     * Create a product version without a release date.
     *
     * @param version describes the version string in semantic versioning format.
     * @return a not yet released product version.
     */
    fun version(version: String): VlVersionValue

    /**
     * Create a product version with a release date.
     *
     * @param version describes the version string in semantic versioning format.
     * @param releaseDate use the format YYYY-MM-dd to specify.
     * @return a released product version.
     *
     */
    fun version(
        version: String,
        releaseDate: String,
    ): VlVersionValue
}
