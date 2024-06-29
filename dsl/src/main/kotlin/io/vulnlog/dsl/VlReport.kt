package io.vulnlog.dsl

import io.vulnlog.core.model.version.VlVersion

interface VlReport {
    fun affectedVersions(vararg affectedVersions: VlVersion)

    fun owasp(vararg affectedVersions: VlVersion)

    fun snyk(
        snykId: String,
        vararg affectedVersions: VlVersion,
        init: (VlSnykBlock.() -> Unit)? = null,
    )
}
