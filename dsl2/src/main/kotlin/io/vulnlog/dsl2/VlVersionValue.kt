package io.vulnlog.dsl2

/**
 * A specific version of the product.
 */
interface VlVersionValue {
    val version: String
    val releaseDate: String?
}
