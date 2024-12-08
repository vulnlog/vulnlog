package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dsl.VlReportForValue
import dev.vulnlog.dsl.VlVariantValue

internal data class VlReportForValueImpl(
    override val variant: VlVariantValue,
    override val release: VlReleaseValue,
) : VlReportForValue
