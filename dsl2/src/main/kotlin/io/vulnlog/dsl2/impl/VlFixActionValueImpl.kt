package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlFixActionValue

internal data class VlFixActionValueImpl(
    override val action: String,
) : VlFixActionValue
