package dev.vulnlog.dsl3

class VlDslVulnImpl : VlDslVuln {
    val vulns = mutableListOf<DummyVuln>()

    override fun vuln(
        name: String,
        release: VlBranch,
    ): DummyVuln {
        val vuln = DummyVuln(name, release)
        vulns += vuln
        return vuln
    }

    override fun toString(): String {
        return "VlDslVulnImpl(vulns=$vulns)"
    }
}
