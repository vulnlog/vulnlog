package io.vulnlog.dsl2

interface VlVariantValue {
    val specifier: String
    val reportedVersions: Set<VlReleaseValue>
}
