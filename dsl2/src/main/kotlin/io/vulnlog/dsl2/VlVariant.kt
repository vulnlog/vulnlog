package io.vulnlog.dsl2

interface VlVariant {
    val specifier: String
    val reportedVersions: Set<VlVersion>
}
