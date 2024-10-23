package io.vulnlog.dsl2

/**
 * A specific version of the product.
 */
interface VlVersion {
    val version: String
    val releaseDate: String?
}
