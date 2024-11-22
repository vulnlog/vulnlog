package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlReleaseValue
import io.vulnlog.dsl.VlReportForValue
import io.vulnlog.dsl.VlVariantValue

internal data class VlReportForValueImpl(
    override val variant: VlVariantValue,
    override val versions: VlReleaseValue,
) : VlReportForValue
