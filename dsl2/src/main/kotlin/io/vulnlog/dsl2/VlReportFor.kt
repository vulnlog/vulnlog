package io.vulnlog.dsl2

interface VlReportFor<out T> {
    /**
     * Define product variant and versions the vulnerability is reported for.
     *
     * @param variant of the product
     * @param versions of the product
     * @return variant and version the vulnerability report addresses.
     */
    fun reportFor(
        variant: VlVariantValue,
        vararg versions: VlReleaseValue,
    )
}
