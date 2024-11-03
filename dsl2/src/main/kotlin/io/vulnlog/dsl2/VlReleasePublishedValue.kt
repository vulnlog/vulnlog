package io.vulnlog.dsl2

/**
 * A specific version of the product.
 */
interface VlReleasePublishedValue : VlReleaseValue {
    val releaseDate: String?
}
