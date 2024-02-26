val v100 = Version(1, 0, 0)
val v101 = Version(1, 0, 1)

val r1 = release { name = "r1"; upComing = v101; published { +v100 } }

branches { +r1 }

vulnerability {
    cve = "cve1"
    reporter {
        owaspDependencyChecker {
            affected { +v100 }
        }
    }
    resolution {
        suppress {
            reason =
                "Version 1.0.0 is not immediately affected. Nevertheless, dependency shall be fixed in upcoming release."
            inVersion { +v100 }
            untilVersion { +v101 }
        }
    }
}
