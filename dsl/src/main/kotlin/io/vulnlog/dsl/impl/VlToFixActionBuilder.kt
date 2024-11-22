package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlFixActionValue

interface VlToFixActionBuilder {
    fun build(): VlFixActionValue
}
