package io.vulnlog.dsl2

interface VlFixIn {
    /**
     * Specify exactly one version per product branch that fixes the reported vulnerability.
     */
    fun fixIn(vararg versions: VlVersion): Set<VlVersion>
}
