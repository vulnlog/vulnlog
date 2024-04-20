val v010 = Version(0, 1, 0)
val v100 = Version(1, 0, 0)

val r1 =
    release {
        name = "release 1"
        upComing = v100
        published { +v010 }
    }

branches { -r1 }

vulnerability {
    cve = "CVE-2019-10782"

    reporter {
        owaspDependencyChecker {
            affected { +v010 }
        }
    }

    resolution {
        suppress {
            reason = "This is a test suppress for demonstration purpose"
        }
    }
}

vulnerability {
    cve = "CVE-2019-9658"

    reporter {
        owaspDependencyChecker {
            affected { +v010 }
        }
    }

    resolution {
        suppress {
            reason = "This is a test suppress for demonstration purpose"
        }
    }
}

vulnerability {
    cve = "CVE-2023-6378"

    reporter {
        owaspDependencyChecker {
            affected { +v010 }
        }
    }

    resolution {
        suppress {
            reason = "This is a test suppress for demonstration purpose"
        }
    }
}
