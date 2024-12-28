package dev.vulnlog.dsl3

interface VlDslVuln {
    fun vuln(
        name: String,
        release: VlBranch,
    ): DummyVuln
}
