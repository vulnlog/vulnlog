package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlReleaseValue
import io.vulnlog.dsl2.VlReportForValue
import io.vulnlog.dsl2.VlVariantValue

internal data class VlReportForValueImpl(
    override val variant: VlVariantValue,
    override val versions: VlReleaseValue,
) : VlReportForValue
