package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlReleaseValue

internal data class VlReleaseValueImpl(
    override val version: String,
) : VlReleaseValue
