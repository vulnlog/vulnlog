package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlReleaseValue

internal data class VlReleaseValueImpl(
    override val version: String,
) : VlReleaseValue
