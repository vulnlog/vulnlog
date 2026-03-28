package dev.vulnlog.cli.core

import com.github.packageurl.PackageURL
import dev.vulnlog.cli.model.Purl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PurlParserTest : FunSpec({

    context("PURL type mapping") {
        test("cargo type maps to Purl.Cargo") {
            parsePurl(PackageURL("pkg:cargo/rand@0.8.5")).shouldBeInstanceOf<Purl.Cargo>()
        }

        test("deb type maps to Purl.Deb") {
            parsePurl(PackageURL("pkg:deb/debian/curl@7.50.3-1")).shouldBeInstanceOf<Purl.Deb>()
        }

        test("docker type maps to Purl.Docker") {
            parsePurl(PackageURL("pkg:docker/nginx@1.25.0")).shouldBeInstanceOf<Purl.Docker>()
        }

        test("gem type maps to Purl.Gem") {
            parsePurl(PackageURL("pkg:gem/rails@7.1.0")).shouldBeInstanceOf<Purl.Gem>()
        }

        test("generic type maps to Purl.Generic") {
            parsePurl(PackageURL("pkg:generic/openssl@3.0.0")).shouldBeInstanceOf<Purl.Generic>()
        }

        test("golang type maps to Purl.Golang") {
            parsePurl(PackageURL("pkg:golang/github.com/gin-gonic/gin@1.9.0")).shouldBeInstanceOf<Purl.Golang>()
        }

        test("maven type maps to Purl.Maven") {
            parsePurl(PackageURL("pkg:maven/org.springframework/spring-core@6.1.0")).shouldBeInstanceOf<Purl.Maven>()
        }

        test("npm type maps to Purl.Npm") {
            parsePurl(PackageURL("pkg:npm/lodash@4.17.21")).shouldBeInstanceOf<Purl.Npm>()
        }

        test("nuget type maps to Purl.Nuget") {
            parsePurl(PackageURL("pkg:nuget/Newtonsoft.Json@13.0.1")).shouldBeInstanceOf<Purl.Nuget>()
        }

        test("pypi type maps to Purl.Pypi") {
            parsePurl(PackageURL("pkg:pypi/requests@2.31.0")).shouldBeInstanceOf<Purl.Pypi>()
        }

        test("rpm type maps to Purl.Rpm") {
            parsePurl(PackageURL("pkg:rpm/redhat/openssl@3.0.0")).shouldBeInstanceOf<Purl.Rpm>()
        }

        test("unknown type falls back to Purl.Generic") {
            parsePurl(PackageURL("pkg:bitbucket/birko/foobar@1.0")).shouldBeInstanceOf<Purl.Generic>()
        }
    }

    context("PURL value is canonicalized") {
        test("maven purl stores the canonicalized string") {
            val purl = PackageURL("pkg:maven/org.apache.log4j/log4j-core@2.14.1")
            parsePurl(purl).value shouldBe purl.canonicalize()
        }

        test("npm purl stores the canonicalized string") {
            val purl = PackageURL("pkg:npm/express@4.18.2")
            parsePurl(purl).value shouldBe purl.canonicalize()
        }

        test("cargo purl stores the canonicalized string") {
            val purl = PackageURL("pkg:cargo/tokio@1.35.0")
            parsePurl(purl).value shouldBe purl.canonicalize()
        }
    }
})
