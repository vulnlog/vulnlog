package dev.vulnlog.dsl

import java.time.LocalDate

data class VlRelease(val version: String, val releaseDate: LocalDate?) {
    companion object {
        /**
         * Define a release with an optional publication date.
         *
         * @since v0.5.0
         */
        fun createRelease(
            version: String,
            publicationDate: String?,
        ): VlRelease {
            return if (publicationDate == null) {
                VlRelease(version, null)
            } else {
                VlRelease(version, LocalDate.parse(publicationDate))
            }
        }
    }
}
