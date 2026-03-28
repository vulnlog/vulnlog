package dev.vulnlog.cli.core

import com.github.packageurl.PackageURL
import dev.vulnlog.cli.model.Purl

fun parsePurl(purl: PackageURL): Purl {
    return when (purl.type.lowercase()) {
        "cargo" -> Purl.Cargo(purl.canonicalize())
        "deb" -> Purl.Deb(purl.canonicalize())
        "docker" -> Purl.Docker(purl.canonicalize())
        "gem" -> Purl.Gem(purl.canonicalize())
        "generic" -> Purl.Generic(purl.canonicalize())
        "golang" -> Purl.Golang(purl.canonicalize())
        "maven" -> Purl.Maven(purl.canonicalize())
        "npm" -> Purl.Npm(purl.canonicalize())
        "nuget" -> Purl.Nuget(purl.canonicalize())
        "pypi" -> Purl.Pypi(purl.canonicalize())
        "rpm" -> Purl.Rpm(purl.canonicalize())
        else -> Purl.Generic(purl.canonicalize())
    }
}
