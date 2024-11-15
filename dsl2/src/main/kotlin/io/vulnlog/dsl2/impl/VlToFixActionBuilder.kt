package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlFixActionValue

interface VlToFixActionBuilder {
    fun build(): VlFixActionValue
}
