package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlActionContext
import io.vulnlog.dsl2.VlNoActionValue
import io.vulnlog.dsl2.VlSuppressionBuilder

internal class VlActionContextImpl : VlActionContext {
    override fun noAction(): VlNoActionValue = VlNoActionValueImpl()

    override fun suppressUntilNextReleaseInBranch(): VlSuppressionBuilder = VlSuppressionBuilder()

    override fun suppressPermanent(): VlSuppressionBuilder = VlSuppressionBuilder()

    override fun suppressTemporarily(untilDate: String): VlSuppressionBuilder = VlSuppressionBuilder()
}
