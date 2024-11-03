package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlActionContext
import io.vulnlog.dsl2.VlNoActionValue
import io.vulnlog.dsl2.VlSuppressionBuilder

class VlActionContextImpl : VlActionContext {
    override fun noAction(): VlNoActionValue = VlNoActionValueImpl()

    override fun suppressUntilNextReleaseInBranch(): VlSuppressionBuilder = VlSuppressionBuilderImpl()

    override fun suppressPermanent(): VlSuppressionBuilder = VlSuppressionBuilderImpl()

    override fun suppressTemporarily(untilDate: String): VlSuppressionBuilder = VlSuppressionBuilderImpl()
}
