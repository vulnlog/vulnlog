package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlFixActionValue
import dev.vulnlog.dsl.VlNoActionValue
import dev.vulnlog.dsl.VlOverwriteValue
import dev.vulnlog.dsl.VlRatingValue
import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dsl.VlReportByValue
import dev.vulnlog.dsl.VlReportForValue
import dev.vulnlog.dsl.VlSuppressionValue

internal data class VlOverwriteValueImpl(
    override val reportedFor: Set<VlReportForValue>,
    override val reportBy: Set<VlReportByValue>,
    override val rating: VlRatingValue?,
    override val toFix: VlFixActionValue?,
    override val fixIn: Set<VlReleaseValue>,
    override val noAction: VlNoActionValue?,
    override val suppressionValue: VlSuppressionValue?,
) : VlOverwriteValue
