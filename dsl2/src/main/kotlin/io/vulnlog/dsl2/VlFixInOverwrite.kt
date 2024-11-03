package io.vulnlog.dsl2

interface VlFixInOverwrite {
    /**
     * Specify exactly one version per product branch that fixes the reported vulnerability.
     */
    fun fixIn(vararg versions: VlReleaseValue): VlOverwriteBuilder
}
