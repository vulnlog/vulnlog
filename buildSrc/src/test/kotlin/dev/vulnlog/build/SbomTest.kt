// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.build

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.cyclonedx.Version
import org.cyclonedx.model.Bom
import org.cyclonedx.model.Component
import org.cyclonedx.model.Dependency
import org.cyclonedx.model.Hash
import org.cyclonedx.model.LifecycleChoice
import org.cyclonedx.model.Metadata
import org.cyclonedx.model.OrganizationalEntity
import org.cyclonedx.model.Property
import org.cyclonedx.model.metadata.ToolInformation
import org.cyclonedx.parsers.JsonParser
import java.io.File

private const val GENERATED_REF = "pkg:maven/dev.vulnlog/cli-app@1.0.0?project_path=%3Acli-app"
private const val LIBRARY_REF = "pkg:maven/org.example/library@2.0.0?type=jar"
private const val IMAGE_PURL = "pkg:oci/vulnlog@sha256%3Accc?repository_url=ghcr.io/vulnlog"

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

private fun component(
    group: String,
    name: String,
): Component =
    Component().apply {
        this.group = group
        this.name = name
        bomRef = "$group:$name"
    }

private fun scanned(
    bomRef: String,
    foundBy: String,
): Component =
    Component().apply {
        this.bomRef = bomRef
        properties = listOf(Property("syft:package:foundBy", foundBy))
    }

private fun File.withProducts(vararg purls: String): File =
    apply {
        val statements =
            purls.joinToString(",") { """{"products": [{"@id": "$it", "identifiers": {"purl": "$it"}}]}""" }
        writeText("""{"statements": [$statements]}""")
    }

class SbomTest :
    FunSpec({

        test("versionedPurl sets the version percent-encoded and keeps the namespace and qualifiers") {
            val purls =
                listOf(
                    "pkg:generic/vulnlog" to "1.0.0",
                    "pkg:maven/dev.vulnlog/gradle-plugin" to "1.0.0+abc",
                    "pkg:oci/vulnlog?repository_url=ghcr.io/vulnlog/vulnlog" to "sha256:abc",
                    "pkg:generic/vulnlog@1.0.0" to null,
                )

            val versioned = purls.map { (purl, version) -> versionedPurl(purl, version) }

            versioned shouldBe
                listOf(
                    "pkg:generic/vulnlog@1.0.0",
                    "pkg:maven/dev.vulnlog/gradle-plugin@1.0.0%2Babc",
                    "pkg:oci/vulnlog@sha256%3Aabc?repository_url=ghcr.io%2Fvulnlog%2Fvulnlog",
                    "pkg:generic/vulnlog",
                )
        }

        test("identifyRootAs replaces the root purl, bom-ref and dependency ref and keeps the graph") {
            val bom = bom()

            bom.identifyRootAs("pkg:generic/vulnlog@1.0.0")

            bom.metadata.component.purl shouldBe "pkg:generic/vulnlog@1.0.0"
            bom.metadata.component.bomRef shouldBe "pkg:generic/vulnlog@1.0.0"
            bom.dependencies.map { it.ref } shouldBe listOf("pkg:generic/vulnlog@1.0.0", LIBRARY_REF)
            bom.dependencies.first().dependencies.map { it.ref } shouldBe listOf(LIBRARY_REF)
        }

        test("checkRootMatchesProductsOf accepts products that differ from the root purl in version only") {
            val bom = bom().apply { metadata.component.purl = IMAGE_PURL }
            val vexDocument =
                tempfile().withProducts(
                    "pkg:oci/vulnlog@sha256%3Aaaa?repository_url=ghcr.io%2Fvulnlog",
                    "pkg:oci/vulnlog@sha256%3Abbb?repository_url=ghcr.io%2Fvulnlog",
                )

            shouldNotThrowAny { bom.checkRootMatchesProductsOf(vexDocument) }
        }

        test("checkRootMatchesProductsOf fails on products with another name or qualifier") {
            val bom = bom().apply { metadata.component.purl = IMAGE_PURL }
            val vexDocument =
                tempfile().withProducts(
                    "pkg:oci/vulnlog@sha256%3Aaaa?repository_url=ghcr.io%2Fvulnlog",
                    "pkg:oci/vulnlog-cli@sha256%3Aaaa?repository_url=ghcr.io%2Fvulnlog",
                    "pkg:oci/vulnlog@sha256%3Aaaa?repository_url=docker.io%2Fvulnlog",
                )

            val failure = shouldThrow<IllegalStateException> { bom.checkRootMatchesProductsOf(vexDocument) }

            failure.message shouldContain "pkg:oci/vulnlog-cli@sha256%3Aaaa?repository_url=ghcr.io%2Fvulnlog"
            failure.message shouldContain "pkg:oci/vulnlog@sha256%3Aaaa?repository_url=docker.io%2Fvulnlog"
        }

        test("writeBom writes a BOM that the CycloneDX schema accepts") {
            val bom =
                Bom().apply {
                    metadata =
                        Metadata().apply {
                            component =
                                Component().apply {
                                    type = Component.Type.APPLICATION
                                    name = "vulnlog"
                                    purl = "pkg:generic/vulnlog@1.0.0"
                                }
                        }
                }
            val bomFile = tempfile()

            bomFile.writeBom(bom, Version.VERSION_17)

            val written = JsonParser().parse(bomFile)
            written.metadata.component.purl shouldBe "pkg:generic/vulnlog@1.0.0"
            written.metadata.timestamp shouldNotBe null
        }

        test("writeBom fails on a BOM that violates the CycloneDX schema") {
            val bom = bom(Component())
            val bomFile = tempfile()

            val failure = shouldThrow<IllegalStateException> { bomFile.writeBom(bom, Version.VERSION_17) }

            failure.message shouldContain "violates the CycloneDX 1.7 schema"
        }

        test("attributeAuthorAndProducers names Vulnlog as SBOM author and records the lifecycle phase") {
            val bom = bom()

            bom.attributeAuthorAndProducers(LifecycleChoice.Phase.BUILD)

            bom.metadata.manufacturer.name shouldBe "Vulnlog"
            bom.metadata.manufacturer.urls shouldBe listOf("https://vulnlog.dev")
            bom.metadata.lifecycles.lifecycleChoice.map { it.phase } shouldBe listOf(LifecycleChoice.Phase.BUILD)
        }

        test("attributeAuthorAndProducers makes Vulnlog producer, supplier and licensor of the root and its group") {
            val bom = bom(component("dev.vulnlog", "lib"), component("org.example", "library"))
            bom.metadata.component.group = "dev.vulnlog"

            bom.attributeAuthorAndProducers(LifecycleChoice.Phase.BUILD)

            val (lib, library) = bom.components
            listOf(bom.metadata.component, lib).forEach {
                it.manufacturer.name shouldBe "Vulnlog"
                it.supplier.name shouldBe "Vulnlog"
                it.licenses.licenses.map { license -> license.id } shouldBe listOf("Apache-2.0")
            }
            listOf(library.manufacturer.name, library.supplier, library.licenses) shouldBe listOf("unknown", null, null)
            bom.metadata.licenses shouldBe null
        }

        test("attributeAuthorAndProducers marks missing producers unknown and keeps named ones, like in Syft's scan") {
            val graalVm = component("com.oracle", "Oracle GraalVM")
            graalVm.manufacturer = OrganizationalEntity().apply { name = "Oracle" }
            val scan = bom(scanned("apk", "apk-db-cataloger"), graalVm)

            scan.attributeAuthorAndProducers(LifecycleChoice.Phase.POST_BUILD)

            scan.metadata.component.manufacturer.name shouldBe "Vulnlog"
            scan.components.map { it.manufacturer.name } shouldBe listOf("unknown", "Oracle")
        }

        test("connectOrphansToRoot makes the root depend on every component nothing else depends on") {
            val scan =
                Bom().apply {
                    metadata = Metadata().apply { component = Component().apply { bomRef = "image" } }
                    components = mutableListOf(scanned("apk-tools", "apk"), scanned("zlib", "apk"), scanned("os", "os"))
                    dependencies =
                        mutableListOf(
                            Dependency("image"),
                            Dependency("apk-tools").apply { addDependency(Dependency("zlib")) },
                        )
                }

            scan.connectOrphansToRoot()

            scan.dependencies.first().dependencies.map { it.ref } shouldBe listOf("apk-tools", "os")
        }

        test("keepSha256HashesOnly drops every other algorithm and keeps components without hashes") {
            val bom = bom(component("MD5", "SHA-256", "SHA-512"), component())

            bom.keepSha256HashesOnly()

            bom.components.first().hashes.map { it.algorithm } shouldBe listOf("SHA-256")
            bom.components.last().hashes.shouldBeEmpty()
        }

        test("addGraalVmComponentsOf adds only GraalVM's components and makes the root depend on them") {
            val bom = bom()
            val nativeImageBom =
                Bom().apply {
                    components =
                        mutableListOf(
                            component("com.oracle", "Oracle GraalVM"),
                            component("org.graalvm.nativeimage", "svm"),
                            component("kotlin.jvm", "internal"),
                        )
                }

            bom.addGraalVmComponentsOf(nativeImageBom)

            val graalVmRefs = listOf("com.oracle:Oracle GraalVM", "org.graalvm.nativeimage:svm")
            bom.components.map { it.bomRef } shouldBe graalVmRefs
            bom.dependencies.map { it.ref } shouldBe listOf(GENERATED_REF, LIBRARY_REF) + graalVmRefs
            bom.dependencies.first().dependencies.map { it.ref } shouldBe listOf(LIBRARY_REF) + graalVmRefs
        }

        test("addGraalVmComponentsOf names Native Image with the GraalVM version as SBOM tool") {
            val bom = bom()
            val nativeImageBom =
                Bom().apply {
                    components = mutableListOf(component("com.oracle", "Oracle GraalVM").apply { version = "25.0.4" })
                }

            bom.addGraalVmComponentsOf(nativeImageBom)

            bom.metadata.toolChoice.components.map { "${it.name} ${it.version}" } shouldBe
                listOf("Oracle GraalVM Native Image 25.0.4")
        }

        test("addGraalVmComponentsOf fails when Native Image's SBOM lists no GraalVM components") {
            val nativeImageBom = Bom().apply { components = mutableListOf(component("kotlin.jvm", "internal")) }

            shouldThrow<IllegalStateException> { bom().addGraalVmComponentsOf(nativeImageBom) }
        }

        test("replaceNativeImageComponentsWith swaps Syft's Native Image components for the native SBOM") {
            val scan =
                Bom().apply {
                    metadata = Metadata().apply { component = Component().apply { bomRef = "image" } }
                    components =
                        mutableListOf(
                            scanned("apk", "apk-db-cataloger"),
                            scanned("misnamed", "graalvm-native-image-cataloger"),
                        )
                    dependencies = mutableListOf(Dependency("apk"), Dependency("misnamed"))
                }

            scan.replaceNativeImageComponentsWith(bom(Component().apply { bomRef = LIBRARY_REF }))

            scan.components.map { it.bomRef } shouldBe listOf("apk", GENERATED_REF, LIBRARY_REF)
            scan.dependencies.map { it.ref } shouldBe listOf("apk", GENERATED_REF, LIBRARY_REF, "image")
            scan.dependencies.last().dependencies.map { it.ref } shouldBe listOf(GENERATED_REF)
        }

        test("replaceNativeImageComponentsWith adds the native SBOM's tools to Syft's") {
            fun tool(name: String) =
                ToolInformation().apply { components = listOf(Component().apply { this.name = name }) }
            val scan =
                Bom().apply {
                    metadata =
                        Metadata().apply {
                            component = Component().apply { bomRef = "image" }
                            toolChoice = tool("syft")
                        }
                    components = mutableListOf(scanned("misnamed", "graalvm-native-image-cataloger"))
                    dependencies = mutableListOf(Dependency("misnamed"))
                }
            val nativeBom = bom().apply { metadata.toolChoice = tool("cyclonedx-gradle-plugin") }

            scan.replaceNativeImageComponentsWith(nativeBom)

            scan.metadata.toolChoice.components.map { it.name } shouldBe listOf("syft", "cyclonedx-gradle-plugin")
        }

        test("replaceNativeImageComponentsWith fails when Syft found no Native Image components") {
            val scan = Bom().apply { components = mutableListOf(scanned("apk", "apk-db-cataloger")) }

            shouldThrow<IllegalStateException> { scan.replaceNativeImageComponentsWith(bom()) }
        }
    })
