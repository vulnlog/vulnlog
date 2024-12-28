package dev.vulnlog.dsl2

interface VlBranchContext {
    infix fun String.publishedAt(publication: String): Pair<VlVersion, VlPublication>

    fun release(vararg release: Pair<VlVersion, VlPublication>)
}
