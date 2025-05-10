package dev.vulnlog.suppression

import Filtered

/**
 * Service responsible for collecting and processing suppression-related vulnerability data.
 *
 * This service integrates with other services such as `VulnerabilitySplitterService` and
 * `SuppressionVulnerabilityMapperService` to aggregate and transform vulnerability data
 * into a consumable form.
 *
 * Key functionalities include splitting reported vulnerabilities, mapping suppression-related
 * vulnerabilities, and ultimately producing a set of relevant results grouped by release branch
 * and reporter.
 *
 * @constructor Instantiates the service with required dependencies.
 * @param vulnerabilitySplitterService A service to partition and group vulnerabilities by relevant criteria.
 * @param suppressionVulnerabilityMapperService A service to map grouped vulnerabilities to relevant suppression
 * information.
 */
class SuppressionCollectorService(
    private val vulnerabilitySplitterService: VulnerabilitySplitterService,
    private val suppressionVulnerabilityMapperService: SuppressionVulnerabilityMapperService,
) {
    /**
     * Collects and processes vulnerabilities per release branch into a meaningful set of results by integrating
     * with the `VulnerabilitySplitterService` and `SuppressionVulnerabilityMapperService`.
     *
     * This method transforms the input containing branch-specific vulnerabilities into a set of grouped
     * and relevant suppression vulnerabilities, indexed by branch and reporter.
     *
     * @param input Input containing branch-specific vulnerability data to be processed, including release branches
     * and associated vulnerabilities.
     * @return A set of processed vulnerabilities grouped by release branch and report origin, containing relevant
     * suppression data.
     */
    fun collect(input: Filtered): Set<VulnsPerBranchAndRecord> {
        val splitterInput: List<SplitterInput> =
            input.vulnerabilitiesPerBranch.map { (branch, vulns) ->
                SplitterInput(branch, vulns)
            }
        val splitVulns = vulnerabilitySplitterService.split(splitterInput)
        return suppressionVulnerabilityMapperService.mapToRelevantVulnerabilities(splitVulns)
    }
}
