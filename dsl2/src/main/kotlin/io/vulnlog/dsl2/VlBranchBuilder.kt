package io.vulnlog.dsl2

interface VlBranchBuilder {
    /**
     * Defines a newer version superseding the previous ones.
     *
     * @param release that replaces all previous versions.
     */
    infix fun supersededBy(release: VlReleaseValue): VlBranchBuilder
}
