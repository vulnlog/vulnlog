package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.VlReleasePublishedValue
import java.time.LocalDate

internal data class VlReleasePublishedValueImpl(
    override val version: String,
    override val releaseDate: LocalDate,
) : VlReleasePublishedValue
