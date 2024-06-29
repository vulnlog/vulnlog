package io.vulnlog.cli.output

import io.vulnlog.cli.suppressions.SuppressionComposition

interface OutputService {
    fun write(suppressionOutputList: List<SuppressionComposition>)
}
