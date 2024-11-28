package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlVariantValue

internal data class VlVariantValueImpl(
    override val specifier: String,
) : VlVariantValue
