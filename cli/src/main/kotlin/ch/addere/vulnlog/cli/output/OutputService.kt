package ch.addere.vulnlog.cli.output

import ch.addere.vulnlog.cli.suppressions.SuppressionComposition

interface OutputService {
    fun write(suppressionOutputList: List<SuppressionComposition>)
}
