package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.VlFixActionValue

interface VlToFixActionBuilder {
    fun build(): VlFixActionValue
}
