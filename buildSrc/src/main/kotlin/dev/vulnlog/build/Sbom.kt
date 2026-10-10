// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.build

import com.github.packageurl.PackageURL
import org.cyclonedx.model.Bom
import org.cyclonedx.model.Dependency
import org.cyclonedx.model.License
import org.cyclonedx.model.LicenseChoice

fun versionedPurl(
    purl: String,
    version: String,
): String {
    val unversioned = PackageURL(purl)
    return PackageURL(unversioned.type, unversioned.namespace, unversioned.name, version, null, null).canonicalize()
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

// SHA-256 alone identifies a file, the seven other algorithms only bloat the document.
fun Bom.keepSha256HashesOnly() {
    components.forEach { it.hashes = it.hashes?.filter { hash -> hash.algorithm == "SHA-256" } }
}
