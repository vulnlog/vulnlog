// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.build

import com.github.packageurl.PackageURL
import org.cyclonedx.model.Bom
import org.cyclonedx.model.Component
import org.cyclonedx.model.Dependency
import org.cyclonedx.model.License
import org.cyclonedx.model.LicenseChoice
import java.util.TreeMap

fun versionedPurl(
    purl: String,
    version: String,
): String {
    val unversioned = PackageURL(purl)
    val qualifiers = unversioned.qualifiers?.let { TreeMap(it) }
    return PackageURL(unversioned.type, unversioned.namespace, unversioned.name, version, qualifiers, null)
        .canonicalize()
}

fun Bom.identifyRootAs(purl: String) {
    val root = metadata.component
    val rootDependency = dependencies.single { it.ref == root.bomRef }
    dependencies[dependencies.indexOf(rootDependency)] =
        Dependency(purl).apply { dependencies = rootDependency.dependencies }
    root.bomRef = purl
    root.purl = purl
}

fun Bom.licenseRootAs(spdxId: String) {
    metadata.component.licenses = LicenseChoice().apply { addLicense(License().apply { id = spdxId }) }
}

// SHA-256 alone identifies a file; the seven other algorithms only bloat the document.
fun Bom.keepSha256HashesOnly() {
    components.forEach { it.hashes = it.hashes?.filter { hash -> hash.algorithm == "SHA-256" } }
}

// The binary also holds GraalVM's JDK and runtime; Native Image misnames the libraries it derives from manifests.
fun Bom.addGraalVmComponentsOf(nativeImageBom: Bom) {
    val graalVm =
        nativeImageBom.components.filter { it.group == "com.oracle" || it.group?.startsWith("org.graalvm.") == true }
    val rootDependency = dependencies.single { it.ref == metadata.component.bomRef }
    graalVm.forEach { component ->
        components.add(component)
        dependencies.add(Dependency(component.bomRef))
        rootDependency.addDependency(Dependency(component.bomRef))
    }
}

// Syft reads the native binary's libraries and their graph from Native Image's embedded SBOM, which misnames them.
fun Bom.replaceNativeImageComponentsWith(nativeBom: Bom) {
    val misnamed = components.filter { it.foundBy() == "graalvm-native-image-cataloger" }.map { it.bomRef }.toSet()
    components.removeIf { it.bomRef in misnamed }
    dependencies.removeIf { it.ref in misnamed }
    val application = nativeBom.metadata.component
    components.add(application)
    components.addAll(nativeBom.components)
    dependencies.addAll(nativeBom.dependencies)
    dependencies.add(Dependency(metadata.component.bomRef).apply { addDependency(Dependency(application.bomRef)) })
}

private fun Component.foundBy(): String? = properties?.find { it.name == "syft:package:foundBy" }?.value
