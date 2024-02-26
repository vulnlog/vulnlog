package ch.addere.dsl

interface VulnLog {
    val releaseBranch: Set<ReleaseBranch>
    val branches: SupportedBranches?
    val vulnerabilities: Set<Vulnerability>
}
