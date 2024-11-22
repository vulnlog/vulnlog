package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlReleaseValue

internal data class VlReleaseValueImpl(
    override val version: String,
) : VlReleaseValue
