package dev.vulnlog.dsl

interface VlFixIn {
    /**
     * Specify exactly one version per product branch that fixes the reported vulnerability.
     */
    fun fixIn(vararg versions: VlReleaseValue)
}
