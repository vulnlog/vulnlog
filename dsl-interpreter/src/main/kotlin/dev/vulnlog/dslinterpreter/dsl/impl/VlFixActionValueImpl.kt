package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlFixActionValue

internal data class VlFixActionValueImpl(
    override val action: String,
) : VlFixActionValue
