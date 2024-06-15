package ch.addere.vulnlog.cli.output

import ch.addere.vulnlog.cli.suppressions.SuppressionComposition
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

class FileOutputService(private val outputDirectory: Path) : OutputService {
    override fun write(suppressionOutputList: List<SuppressionComposition>) {
        suppressionOutputList.forEach { suppressionComposition ->
            File("${outputDirectory.pathString}/${suppressionComposition.outputFileName}").printWriter()
                .use { out -> out.print(suppressionComposition.prettyString()) }
        }
    }
}
