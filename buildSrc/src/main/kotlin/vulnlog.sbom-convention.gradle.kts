import dev.vulnlog.build.SbomExtension
import dev.vulnlog.build.addGraalVmComponentsOf
import dev.vulnlog.build.identifyRootAs
import dev.vulnlog.build.keepSha256HashesOnly
import dev.vulnlog.build.licenseRootAs
import dev.vulnlog.build.versionedPurl
import org.cyclonedx.Version
import org.cyclonedx.generators.BomGeneratorFactory
import org.cyclonedx.parsers.JsonParser
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask

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

pluginManager.withPlugin("org.graalvm.buildtools.native") {
    val nativeCompile = tasks.named<BuildNativeImageTask>("nativeCompile")
    val cyclonedxNativeBom =
        tasks.register("cyclonedxNativeBom") {
            description = "Generates the CycloneDX SBOM of the native executable. Requires Oracle GraalVM."
            val imageName = nativeCompile.flatMap { it.executableShortName }
            val jvmBomFile = tasks.cyclonedxDirectBom.flatMap { it.jsonOutput }
            val schema = tasks.cyclonedxDirectBom.flatMap { it.schemaVersion }
            val nativeImageBomFile =
                nativeCompile.flatMap { it.outputDirectory.file(imageName.map { name -> "$name.sbom.json" }) }
            val projectVersion = version.toString()
            val nativeBomFile =
                layout.buildDirectory.file(imageName.map { "sbom/$it-native-$projectVersion.cdx.json" })
            inputs.file(jvmBomFile)
            inputs.file(nativeImageBomFile)
            outputs.file(nativeBomFile)
            doLast {
                val bom = JsonParser().parse(jvmBomFile.get().asFile)
                bom.addGraalVmComponentsOf(JsonParser().parse(nativeImageBomFile.get().asFile))
                nativeBomFile.get().asFile.writeText(BomGeneratorFactory.createJson(schema.get(), bom).toJsonString())
            }
        }
    gradle.taskGraph.whenReady {
        if (hasTask(cyclonedxNativeBom.get())) {
            nativeCompile.get().options.get().buildArgs.add("--enable-sbom=embed,export")
        }
    }
}
