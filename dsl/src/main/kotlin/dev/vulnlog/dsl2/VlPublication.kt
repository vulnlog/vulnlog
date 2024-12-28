package dev.vulnlog.dsl2

import java.time.LocalDate

interface VlPublication {
    val publication: LocalDate
}

object VlPublicationDefault : VlPublication {
    override val publication: LocalDate = LocalDate.MAX
}

data class VlPublicationImpl(override val publication: LocalDate) : VlPublication
