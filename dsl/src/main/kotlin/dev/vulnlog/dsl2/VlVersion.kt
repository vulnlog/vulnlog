package dev.vulnlog.dsl2

interface VlVersion {
    val version: String
}

object VlVersionDefault : VlVersion {
    override val version: String = "Default Version"
}

data class VlVersionImpl(override val version: String) : VlVersion
