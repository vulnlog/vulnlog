package dev.vulnlog.dsl

interface VlOverwrite<out T> {
    /**
     * Overwrite the default definitions.
     *
     * Overwrite allows to create vulnerability analysis for specific versions and variants of the product.
     *
     * @param variant of the product
     * @param versions of the product
     * @return overwrite builder for a specific variant and version.
     */
    fun overwrite(
        variant: VlVariantValue,
        vararg versions: VlReleaseValue,
    ): T
}
