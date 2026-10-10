import dev.vulnlog.build.SbomExtension
import dev.vulnlog.build.identifyRootAs
import dev.vulnlog.build.keepSha256HashesOnly
import dev.vulnlog.build.licenseRootAs
import dev.vulnlog.build.versionedPurl
import org.cyclonedx.Version
import org.cyclonedx.generators.BomGeneratorFactory
import org.cyclonedx.parsers.JsonParser

plugins {
    id("org.cyclonedx.bom")
}

val sbom = extensions.create<SbomExtension>("sbom")

tasks.cyclonedxDirectBom {
    includeConfigs = listOf("runtimeClasspath")
    schemaVersion = Version.VERSION_17
    // Only the JSON is post-processed and published.
    xmlOutput.unsetConvention()
    val projectVersion = version.toString()
    val rootPurl = sbom.rootPurl.map { versionedPurl(it, projectVersion) }
    inputs.property("rootPurl", rootPurl)
    doLast {
        val bomFile = jsonOutput.get().asFile
        val bom = JsonParser().parse(bomFile)
        bom.identifyRootAs(rootPurl.get())
        bom.licenseRootAs("Apache-2.0")
        bom.keepSha256HashesOnly()
        bomFile.writeText(BomGeneratorFactory.createJson(schemaVersion.get(), bom).toJsonString())
    }
}
