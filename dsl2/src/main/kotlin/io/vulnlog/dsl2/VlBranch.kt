package io.vulnlog.dsl2

interface VlBranch {
    /**
     * Create a product branch containing several release versions.
     *
     * A product branch is a group of sequential releases with a product life cycle.
     *
     * @param name of the release branch.
     * @param initialVersion specifies the starting point of this release branch.
     * @param lifeCycle specifies the sequential phases this product branch passes through.
     */
    fun branch(
        name: String,
        initialVersion: VlReleaseValue,
        lifeCycle: VlLifeCycleValue,
    ): VlBranchBuilder
}
