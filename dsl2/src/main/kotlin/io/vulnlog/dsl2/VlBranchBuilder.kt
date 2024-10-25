package io.vulnlog.dsl2

interface VlBranchBuilder {
    /**
     * Defines a newer version superseding the previous ones.
     *
     * @param version that replaces all previous versions.
     * @param a branch builder.
     */
    infix fun supersededBy(version: VlVersionValue): VlBranchBuilder
}
