package dev.vulnlog.dsl2.impl

import dev.vulnlog.dsl2.VlVulnlogContext

interface VlVulnlogContextBuilder : VlVulnlogContext {
    fun build(): Vulnlog2FileData
}
