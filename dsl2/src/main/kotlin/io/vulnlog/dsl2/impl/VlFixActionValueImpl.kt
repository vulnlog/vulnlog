package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlFixActionValue

data class VlFixActionValueImpl(
    override val action: String,
) : VlFixActionValue
