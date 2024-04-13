package ch.addere.dsl

data class SupportedBranches(
    val supported: Set<ReleaseBranch>,
    val unsupported: Set<ReleaseBranch>,
)

class SupportedBranchesBuilder {
    val supportedBranches = mutableSetOf<ReleaseBranch>()
    val unsupportedBranches = mutableSetOf<ReleaseBranch>()

    operator fun ReleaseBranch.unaryPlus() = supportedBranches.add(this)

    operator fun ReleaseBranch.unaryMinus() = unsupportedBranches.add(this)

    fun build(): SupportedBranches {
        return SupportedBranches(supportedBranches, unsupportedBranches)
    }
}
