package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlFixActionValue

internal data class VlFixActionValueImpl(
    override val action: String,
) : VlFixActionValue
