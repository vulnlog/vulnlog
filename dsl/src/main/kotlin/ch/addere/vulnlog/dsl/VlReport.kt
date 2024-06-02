package ch.addere.vulnlog.dsl

import ch.addere.vulnlog.core.model.version.VlVersion

interface VlReport {
    fun affectedVersions(vararg affectedVersions: VlVersion)

    fun owasp(vararg affectedVersions: VlVersion)

    fun snyk(
        snykId: String,
        vararg affectedVersions: VlVersion,
        init: (VlSnykBlock.() -> Unit)? = null,
    )
}
