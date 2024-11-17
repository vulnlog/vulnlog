package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlReleasePublishedValue
import java.time.LocalDate

internal data class VlReleasePublishedValueImpl(
    override val version: String,
    override val releaseDate: LocalDate,
) : VlReleasePublishedValue
