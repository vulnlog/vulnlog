package dev.vulnlog.dsl3

import java.time.LocalDate

data class VlRelease(val version: String, val releaseDate: LocalDate?) {
    companion object {
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
