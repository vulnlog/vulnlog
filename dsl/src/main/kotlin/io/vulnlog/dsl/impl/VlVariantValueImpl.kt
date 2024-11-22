package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlVariantValue

internal data class VlVariantValueImpl(
    override val specifier: String,
) : VlVariantValue
