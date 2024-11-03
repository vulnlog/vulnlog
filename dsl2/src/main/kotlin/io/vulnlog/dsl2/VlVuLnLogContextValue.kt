package io.vulnlog.dsl2

import io.vulnlog.dsl2.definition.VlDsl
import io.vulnlog.dsl2.definition.VlDslMarker

interface VlVuLnLogContextValue :
    VlDsl,
    VlDslMarker,
    VlRelease,
    VlLifeCycle,
    VlBranch,
    VlVariant,
    VlReporter,
    VlVulnerability {
    /**
     * Name of the vendor of the products this vulnerability log is for.
     */
    var vendorName: String?

    /**
     * Name of the product this vulnerability log is for.
     */
    var productName: String?
}
