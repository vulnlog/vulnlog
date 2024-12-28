package dev.vulnlog.dsl2

interface VlAnalyse {
    fun analyse(ratings: VlVulnerabilityRateContext): MyAnalysis

    fun analyse(vararg ratings: VlVulnerabilityRateContext): Array<MyAnalysis>
}
