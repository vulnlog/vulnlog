package ch.addere.dsl

data class VulnLogs(
    val branches: SupportedBranches?,
    val vulnerabilities: Set<Vulnerability>,
)

class VulnLogsBuilder : V() {
    var releaseBranch = mutableSetOf<ReleaseBranch>()
    var branches: SupportedBranches? = null
    val vulnerabilities = mutableSetOf<Vulnerability>()

    fun release(block: ReleaseBranchBuilder.() -> Unit): ReleaseBranch {
        val builder = ReleaseBranchBuilder()
        builder.block()
        val rb = builder.build()
        releaseBranch += rb
        return rb
    }

    fun branches(block: SupportedBranchesBuilder.() -> Unit) {
        val supportedBranchesBuilder = SupportedBranchesBuilder()
        supportedBranchesBuilder.block()
        branches = supportedBranchesBuilder.build()
    }

    fun vulnerability(block: VulnerabilityBuilder.() -> Unit) {
        val builder = VulnerabilityBuilder()
        builder.block()
        vulnerabilities.add(builder.build())
    }

    val resultBranches = if (branches != null) branches else SupportedBranches(emptySet(), emptySet())

    fun build(): VulnLogs = VulnLogs(resultBranches!!, vulnerabilities)
}
