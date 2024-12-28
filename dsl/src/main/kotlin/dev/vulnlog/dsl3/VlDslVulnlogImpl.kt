package dev.vulnlog.dsl3

class VlDslVulnlogImpl :
    VlDslVulnlog,
    VlDslReleases by VlDslReleasesImpl(),
    VlDslVuln by VlDslVulnImpl()
