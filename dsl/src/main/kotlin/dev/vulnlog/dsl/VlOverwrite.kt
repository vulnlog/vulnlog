package dev.vulnlog.dsl

interface VlOverwrite<out T> {
    /**
     * Overwrite or add release-specific definitions.
     *
     * Allows to define vulnerability analysis more specifically for a particular release.
     * For example, use overrides if
     *   - several releases are vulnerable, but the current development branch has been patched immediately and is
     *   therefore not vulnerable.
     *   - multiple releases need to update dependency version Y to version Z, but one release needs to update
     *   dependency version X to Z.
     *
     * If the specified variant version tuple exists in the vulnerability definition, it is overwritten.
     *
     * @param variant of the product
     * @param versions of the product
     * @return overwrite builder for a specific variant and version.
     */
    fun overwrite(
        variant: VlVariantValue,
        vararg versions: VlReleaseValue,
    ): T
}
