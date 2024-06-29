package io.vulnlog.core.model.version

data class VlReleaseGroup(
    val name: String,
    val upcomingRelease: VlVersion,
    val releases: List<VlVersion>,
) {
    init {
        require(!releases.contains(upcomingRelease)) {
            "Upcoming release must not be within release list: ${upcomingRelease.version}"
        }
        require(releases.toSet().size == releases.size) {
            val duplicates =
                releases
                    .filter { version -> releases.count { it == version } > 1 }
                    .distinct()
                    .toList()
            val versions = duplicates.joinToString { it.version }
            throw IllegalArgumentException("Duplicate Version are not allowed: $versions")
        }
    }

    fun successorsAsString(): String {
        val successors = mutableListOf(upcomingRelease)
        successors.addAll(releases)
        return successors.joinToString("' is the successor of '", "'", "'") { it.version }
    }
}
