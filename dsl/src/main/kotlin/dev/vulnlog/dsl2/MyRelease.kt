package dev.vulnlog.dsl2

interface MyRelease {
    val version: VlVersion
    val publication: VlPublication
}

object DefaultMyRelease : MyRelease {
    override val version: VlVersion = VlVersionDefault
    override val publication: VlPublication = VlPublicationDefault
}

data class MyReleaseImpl(override val version: VlVersion, override val publication: VlPublication) : MyRelease
