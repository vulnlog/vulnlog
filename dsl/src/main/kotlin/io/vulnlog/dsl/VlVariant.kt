package io.vulnlog.dsl

interface VlVariant {
    /**
     * Create one or multiple product variants.
     *
     * Product variants specify more specifically which variant is affected. Variants are sub- or supersets of the
     * product. For example if the product is available as self-contained application and also as a containerised image
     * these are two variants.
     *
     * @param productVariant describes a variation of the product.
     * @return an array of product variants.
     */
    fun variants(vararg productVariant: String): Array<VlVariantValue>
}
