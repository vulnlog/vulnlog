package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlReleasePublishedValue

internal data class VlReleasePublishedValueImpl(
    override val version: String,
    override val releaseDate: String? = null,
) : VlReleasePublishedValue
