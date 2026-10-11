
import dev.vulnlog.build.SbomExtension
import dev.vulnlog.build.addGraalVmComponentsOf
import dev.vulnlog.build.checkRootMatchesProductsOf
import dev.vulnlog.build.identifyRootAs
import dev.vulnlog.build.keepSha256HashesOnly
import dev.vulnlog.build.licenseRootAs
import dev.vulnlog.build.replaceNativeImageComponentsWith
import dev.vulnlog.build.versionedPurl
import dev.vulnlog.build.writeBom
import org.cyclonedx.Version
import org.cyclonedx.parsers.JsonParser
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask

plugins {
    base
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
    val vexDocument = sbom.vexDocument
    inputs.property("rootPurl", rootPurl)
    inputs.file(vexDocument)
    doLast {
        val bomFile = jsonOutput.get().asFile
        val bom = JsonParser().parse(bomFile)
        bom.identifyRootAs(rootPurl.get())
        bom.checkRootMatchesProductsOf(vexDocument.get().asFile)
        bom.licenseRootAs("Apache-2.0")
        bom.keepSha256HashesOnly()
        bomFile.writeBom(bom, schemaVersion.get())
    }
}

tasks.check {
    dependsOn(tasks.cyclonedxDirectBom)
}

pluginManager.withPlugin("org.graalvm.buildtools.native") {
    val nativeCompile = tasks.named<BuildNativeImageTask>("nativeCompile")
    val imageName = nativeCompile.flatMap { it.executableShortName }
    val schema = tasks.cyclonedxDirectBom.flatMap { it.schemaVersion }
    val projectVersion = version.toString()
    val nativeBomFile = layout.buildDirectory.file(imageName.map { "sbom/$it-native-$projectVersion.cdx.json" })
    val cyclonedxNativeBom =
        tasks.register("cyclonedxNativeBom") {
            description = "Generates the CycloneDX SBOM of the native executable. Requires Oracle GraalVM."
            val jvmBomFile = tasks.cyclonedxDirectBom.flatMap { it.jsonOutput }
            val nativeImageBomFile =
                nativeCompile.flatMap { it.outputDirectory.file(imageName.map { name -> "$name.sbom.json" }) }
            inputs.file(jvmBomFile)
            inputs.file(nativeImageBomFile)
            outputs.file(nativeBomFile)
            doLast {
                val bom = JsonParser().parse(jvmBomFile.get().asFile)
                bom.addGraalVmComponentsOf(JsonParser().parse(nativeImageBomFile.get().asFile))
                nativeBomFile.get().asFile.writeBom(bom, schema.get())
            }
        }
    tasks.register("cyclonedxContainerBom") {
        description = "Generates the CycloneDX SBOM of the container image from its Syft scan and the native SBOM."
        val imagePurl = sbom.containerPurl.zip(providers.gradleProperty("imageDigest"), ::versionedPurl)
        val vexDocument = sbom.containerVexDocument
        val scanFile = layout.buildDirectory.file("sbom/syft-container.cdx.json")
        val containerBomFile =
            layout.buildDirectory.file(imageName.map { "sbom/$it-container-$projectVersion.cdx.json" })
        mustRunAfter(cyclonedxNativeBom)
        inputs.property("imagePurl", imagePurl)
        inputs.file(vexDocument)
        inputs.file(scanFile)
        inputs.file(nativeBomFile)
        outputs.file(containerBomFile)
        doLast {
            val bom = JsonParser().parse(scanFile.get().asFile)
            bom.replaceNativeImageComponentsWith(JsonParser().parse(nativeBomFile.get().asFile))
            bom.identifyRootAs(imagePurl.get())
            bom.checkRootMatchesProductsOf(vexDocument.get().asFile)
            containerBomFile.get().asFile.writeBom(bom, schema.get())
        }
    }
    gradle.taskGraph.whenReady {
        if (hasTask(cyclonedxNativeBom.get())) {
            nativeCompile.get().options.get().buildArgs.add("--enable-sbom=embed,export")
        }
    }
}
