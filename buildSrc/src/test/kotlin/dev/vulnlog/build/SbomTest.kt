// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.build

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.cyclonedx.model.Bom
import org.cyclonedx.model.Component
import org.cyclonedx.model.Dependency
import org.cyclonedx.model.Hash
import org.cyclonedx.model.Metadata

private const val GENERATED_REF = "pkg:maven/dev.vulnlog/cli-app@1.0.0?project_path=%3Acli-app"
private const val LIBRARY_REF = "pkg:maven/org.example/library@2.0.0?type=jar"

private fun bom(vararg components: Component): Bom =
    Bom().apply {
        metadata =
            Metadata().apply {
                component =
                    Component().apply {
                        bomRef = GENERATED_REF
                        purl = GENERATED_REF
                    }
            }
        this.components = components.toMutableList()
        dependencies =
            mutableListOf(
                Dependency(GENERATED_REF).apply { addDependency(Dependency(LIBRARY_REF)) },
                Dependency(LIBRARY_REF),
            )
    }

private fun component(vararg algorithms: String): Component =
    Component().apply { hashes = algorithms.map { Hash(it, "digest") } }

class SbomTest :
    FunSpec({

        test("versionedPurl adds the version percent-encoded and keeps the namespace") {
            val purls = listOf("pkg:generic/vulnlog" to "1.0.0", "pkg:maven/dev.vulnlog/gradle-plugin" to "1.0.0+abc")

            val versioned = purls.map { (purl, version) -> versionedPurl(purl, version) }

            versioned shouldBe listOf("pkg:generic/vulnlog@1.0.0", "pkg:maven/dev.vulnlog/gradle-plugin@1.0.0%2Babc")
        }

        test("identifyRootAs replaces the root purl, bom-ref and dependency ref and keeps the graph") {
            val bom = bom()

            bom.identifyRootAs("pkg:generic/vulnlog@1.0.0")

            bom.metadata.component.purl shouldBe "pkg:generic/vulnlog@1.0.0"
            bom.metadata.component.bomRef shouldBe "pkg:generic/vulnlog@1.0.0"
            bom.dependencies.map { it.ref } shouldBe listOf("pkg:generic/vulnlog@1.0.0", LIBRARY_REF)
            bom.dependencies.first().dependencies.map { it.ref } shouldBe listOf(LIBRARY_REF)
        }

        test("licenseRootAs licenses the root component, not the document") {
            val bom = bom()

            bom.licenseRootAs("Apache-2.0")

            bom.metadata.component.licenses.licenses.map { it.id } shouldBe listOf("Apache-2.0")
            bom.metadata.licenses shouldBe null
        }

        test("keepSha256HashesOnly drops every other algorithm and keeps components without hashes") {
            val bom = bom(component("MD5", "SHA-256", "SHA-512"), component())

            bom.keepSha256HashesOnly()

            bom.components.first().hashes.map { it.algorithm } shouldBe listOf("SHA-256")
            bom.components.last().hashes.shouldBeEmpty()
        }
    })
