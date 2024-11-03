package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlReleaseValue
import io.vulnlog.dsl2.VlVariantValue

internal data class VlVariantValueImpl(
    override val specifier: String,
    override val reportedVersions: Set<VlReleaseValue>,
) : VlVariantValue
