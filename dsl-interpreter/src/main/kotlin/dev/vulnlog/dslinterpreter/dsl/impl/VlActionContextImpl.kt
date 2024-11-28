package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlActionContext
import dev.vulnlog.dsl.VlNoActionValue
import dev.vulnlog.dsl.VlSuppressionBuilder

internal class VlActionContextImpl : VlActionContext {
    override fun noAction(): VlNoActionValue = VlNoActionValueImpl()

    override fun suppressUntilNextReleaseInBranch(): VlSuppressionBuilder = VlSuppressionBuilder()

    override fun suppressPermanent(): VlSuppressionBuilder = VlSuppressionBuilder()

    override fun suppressTemporarily(untilDate: String): VlSuppressionBuilder = VlSuppressionBuilder()
}
