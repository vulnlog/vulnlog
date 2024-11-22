package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlActionContext
import io.vulnlog.dsl.VlNoActionValue
import io.vulnlog.dsl.VlSuppressionBuilder

internal class VlActionContextImpl : VlActionContext {
    override fun noAction(): VlNoActionValue = VlNoActionValueImpl()

    override fun suppressUntilNextReleaseInBranch(): VlSuppressionBuilder = VlSuppressionBuilder()

    override fun suppressPermanent(): VlSuppressionBuilder = VlSuppressionBuilder()

    override fun suppressTemporarily(untilDate: String): VlSuppressionBuilder = VlSuppressionBuilder()
}
