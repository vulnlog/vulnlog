package dev.vulnlog.dsl

interface VlOverwriteValue {
    val reportedFor: Set<VlReportForValue>
    val reportBy: Set<VlReportByValue>
    val rating: VlRatingValue?
    val toFix: VlFixActionValue?
    val fixIn: Set<VlReleaseValue>
    val noAction: VlNoActionValue?
    val suppressionValue: VlSuppressionValue?
}
