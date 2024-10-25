package io.vulnlog.dsl2

import io.vulnlog.dsl2.definition.VlDsl
import io.vulnlog.dsl2.definition.VlDslMarker

interface VlVulnLogContext :
    VlDsl,
    VlDslMarker {
    /**
     * Name of the vendor of the products this vulnerability log is for.
     */
    var vendorName: String?

    /**
     * Name of the product this vulnerability log is for.
     */
    var productName: String?

    /**
     * Create a product version without a release date.
     *
     * @param version describes the version string in semantic versioning format.
     * @return a not yet released product version.
     */
    fun version(version: String): VlVersion

    /**
     * Create a product version with a release date.
     *
     * @param version describes the version string in semantic versioning format.
     * @param releaseDate use the format YYYY-MM-dd to specify.
     * @return a released product version.
     *
     */
    fun version(
        version: String,
        releaseDate: String,
    ): VlVersion

    /**
     * Create a product branch life cycle phase describing a relative time range a product branch is a life.
     *
     * @param name of the life cycle phase.
     */
    fun lifeCyclePhase(name: String): VlLifeCycleFromBuilder

    /**
     * Create a complete product branch life cycle of multiple life cycle phases.
     *
     * @param lifeCyclePhases specifies the sequential phases this life cycle contains.
     * @return complete product branch life cycle.
     */
    fun lifeCycle(vararg lifeCyclePhases: VlLifeCycleToBuilder): VlLifeCycle

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
        initialVersion: VlVersion,
        lifeCycle: VlLifeCycle,
    ): VlBranchBuilder

    /**
     * Create one or multiple product variants.
     *
     * Product variants specify more specifically which variant is affected. Variants are sub- or supersets of the
     * product. For example if the product is available as self-contained application and also as a containerised image
     * these are two variants.
     *
     * @param productVariant describes a variation of the product.
     * @return an array of product variants.
     */
    fun variants(vararg productVariant: String): Array<VlVariant>

    /**
     * Create on or multiple reporter that can report vulnerability findings.
     *
     * @param reporterName describes a reporter reporting a vulnerability.
     * @return an array of vulnerability reporter.
     */
    fun reporter(vararg reporterName: String): Array<VlReporter>

    /**
     * Create a vulnerability entry in the log.
     *
     * @param vulnerabilityId one or multiple vulnerability identifiers describing the same vulnerability.
     * @param context describing the vulnerability.
     * @return representation of a vulnerability.
     */
    fun vuln(
        vararg vulnerabilityId: String,
        context: VlVulnerabilityContext.() -> Unit,
    ): VlVulnerability
}
