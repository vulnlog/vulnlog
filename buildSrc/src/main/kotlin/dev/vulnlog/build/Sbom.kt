// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.build

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.packageurl.PackageURL
import org.cyclonedx.Version
import org.cyclonedx.generators.BomGeneratorFactory
import org.cyclonedx.model.Bom
import org.cyclonedx.model.Component
import org.cyclonedx.model.Dependency
import org.cyclonedx.model.License
import org.cyclonedx.model.LicenseChoice
import org.cyclonedx.model.LifecycleChoice
import org.cyclonedx.model.Lifecycles
import org.cyclonedx.model.OrganizationalEntity
import org.cyclonedx.model.metadata.ToolInformation
import org.cyclonedx.parsers.JsonParser
import java.io.File
import java.util.Date
import java.util.TreeMap

fun versionedPurl(
    purl: String,
    version: String?,
): String =
    PackageURL(purl)
        .run { PackageURL(type, namespace, name, version, qualifiers?.let { TreeMap(it) }, subpath) }
        .canonicalize()

fun Bom.identifyRootAs(purl: String) {
    val root = metadata.component
    val rootDependency = dependencies.single { it.ref == root.bomRef }
    dependencies[dependencies.indexOf(rootDependency)] =
        Dependency(purl).apply { dependencies = rootDependency.dependencies }
    root.bomRef = purl
    root.purl = purl
}

// Scanners apply a VEX statement only when its product purl is the SBOM's root purl.
fun Bom.checkRootMatchesProductsOf(vexDocument: File) {
    val root = metadata.component.purl
    val mismatched =
        ObjectMapper()
            .readTree(vexDocument)["statements"]
            .flatMap { it["products"] }
            .map { it["identifiers"]["purl"].asText() }
            .filter { versionedPurl(it, null) != versionedPurl(root, null) }
            .distinct()
    check(mismatched.isEmpty()) { "The products $mismatched of ${vexDocument.name} do not match the root purl $root" }
}

fun File.writeBom(
    bom: Bom,
    schema: Version,
) {
    // The timestamp records the latest change to the SBOM data, and the post-processing changes it.
    bom.metadata.timestamp = Date()
    writeText(BomGeneratorFactory.createJson(schema, bom).toJsonString())
    val violations = JsonParser().validate(this, schema)
    check(violations.isEmpty()) {
        val messages = violations.joinToString { it.message.orEmpty() }
        "$name violates the CycloneDX ${schema.versionString} schema: $messages"
    }
}

// The CISA minimum elements ask for the SBOM author, the generation context, and each component's producer, stated
// as unknown when nothing names it. The CycloneDX plugin finds neither producer nor license for first-party components.
fun Bom.attributeAuthorAndProducers(phase: LifecycleChoice.Phase) {
    metadata.manufacturer = VULNLOG
    metadata.lifecycles =
        Lifecycles().apply { lifecycleChoice = listOf(LifecycleChoice().apply { this.phase = phase }) }
    val root = metadata.component
    val (firstParty, thirdParty) = components.partition { it.group != null && it.group == root.group }
    (firstParty + root).forEach { component ->
        component.manufacturer = VULNLOG
        component.supplier = VULNLOG
        component.licenses = LicenseChoice().apply { addLicense(License().apply { id = LICENSE }) }
    }
    thirdParty.filter { it.manufacturer == null }.forEach { it.manufacturer = UNKNOWN_PRODUCER }
}

// Syft leaves the image's packages unconnected to the image; a consumer walking the graph from the root misses them.
fun Bom.connectOrphansToRoot() {
    val dependedOn = dependencies.flatMap { it.dependencies.orEmpty() }.map { it.ref }.toSet()
    val rootDependency = dependencies.single { it.ref == metadata.component.bomRef }
    components.filter { it.bomRef !in dependedOn }.forEach { rootDependency.addDependency(Dependency(it.bomRef)) }
}

// SHA-256 alone identifies a file; the seven other algorithms only bloat the document.
fun Bom.keepSha256HashesOnly() {
    components.forEach { it.hashes = it.hashes?.filter { hash -> hash.algorithm == "SHA-256" } }
}

// The binary also holds GraalVM's JDK and runtime; Native Image misnames the libraries it derives from manifests.
fun Bom.addGraalVmComponentsOf(nativeImageBom: Bom) {
    val graalVm =
        nativeImageBom.components.filter { it.group == "com.oracle" || it.group?.startsWith("org.graalvm.") == true }
    check(graalVm.isNotEmpty()) { "Native Image's SBOM lists no GraalVM components" }
    val rootDependency = dependencies.single { it.ref == metadata.component.bomRef }
    graalVm.forEach { component ->
        components.add(component)
        dependencies.add(Dependency(component.bomRef))
        rootDependency.addDependency(Dependency(component.bomRef))
    }
    addTools(
        Component().apply {
            type = Component.Type.APPLICATION
            name = "Oracle GraalVM Native Image"
            version = graalVm.single { it.group == "com.oracle" }.version
        },
    )
}

// Syft reads the native binary's libraries and their graph from Native Image's embedded SBOM, which misnames them.
fun Bom.replaceNativeImageComponentsWith(nativeBom: Bom) {
    val misnamed = components.filter { it.foundBy() == NATIVE_IMAGE_CATALOGER }.map { it.bomRef }.toSet()
    check(misnamed.isNotEmpty()) { "Syft's scan has no components found by $NATIVE_IMAGE_CATALOGER" }
    components.removeIf { it.bomRef in misnamed }
    dependencies.removeIf { it.ref in misnamed }
    val application = nativeBom.metadata.component
    components.add(application)
    components.addAll(nativeBom.components)
    dependencies.addAll(nativeBom.dependencies)
    dependencies.add(Dependency(metadata.component.bomRef).apply { addDependency(Dependency(application.bomRef)) })
    addTools(*nativeBom.metadata.toolChoice?.components.orEmpty().toTypedArray())
}

private fun Bom.addTools(vararg tools: Component) {
    metadata.toolChoice = (metadata.toolChoice ?: ToolInformation()).apply { components = components.orEmpty() + tools }
}

private const val LICENSE = "Apache-2.0"

private val VULNLOG =
    OrganizationalEntity().apply {
        name = "Vulnlog"
        urls = listOf("https://vulnlog.dev")
    }

private val UNKNOWN_PRODUCER = OrganizationalEntity().apply { name = "unknown" }

private const val NATIVE_IMAGE_CATALOGER = "graalvm-native-image-cataloger"

private fun Component.foundBy(): String? = properties?.find { it.name == "syft:package:foundBy" }?.value
