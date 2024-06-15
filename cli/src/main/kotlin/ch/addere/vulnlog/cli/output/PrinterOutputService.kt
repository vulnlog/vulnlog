package ch.addere.vulnlog.cli.output

import ch.addere.vulnlog.cli.suppressions.SuppressionComposition

class PrinterOutputService(private val out: (Any?, Boolean, Boolean) -> Unit) : OutputService {
    override fun write(suppressionOutputList: List<SuppressionComposition>) =
        suppressionOutputList.forEach { out(it.prettyString(), true, false) }
}
