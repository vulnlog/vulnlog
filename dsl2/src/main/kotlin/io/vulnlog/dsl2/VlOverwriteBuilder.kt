package io.vulnlog.dsl2

interface VlOverwriteBuilder :
    VlReportBy<VlOverwriteBuilder>,
    VlRating<VlOverwriteBuilder>,
    VlToFixAction<VlOverwriteBuilder>,
    VlFixIn,
    VlActionContext<VlActionValue>
