package dev.vulnlog.dsl

import java.time.LocalDate

/**
 * A specific version of the product.
 */
interface VlReleasePublishedValue : VlReleaseValue {
    val releaseDate: LocalDate
}
