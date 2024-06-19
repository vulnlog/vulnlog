val v100 = version("1.0.0")
val v010 = version("0.1.0")

val r1 = branch("release 1.0", v100, v010)

cve("CVE-2023-6378") {
    snyk("SNYK-JAVA-CHQOSLOGBACK-6094942", v010)
    suppress("Is a Gradle build dependency only and is not exposed in the release build. No Update available.")
}

cve("CVE-2023-6481") {
    snyk("SNYK-JAVA-CHQOSLOGBACK-6097492", v010)
    suppress("Is a Gradle build dependency only and is not exposed in the release build. No Update available.")
}

cve("CVE-2023-6378") {
    snyk("SNYK-JAVA-CHQOSLOGBACK-6094943", v010)
    suppress("Is a Gradle build dependency only and is not exposed in the release build. No Update available.")
}

cve("CVE-2023-6481") {
    snyk("SNYK-JAVA-CHQOSLOGBACK-6097493", v010)
    suppress("Is a Gradle build dependency only and is not exposed in the release build. No Update available.")
}

cve("CVE-2020-29582") {
    snyk("SNYK-JAVA-ORGJETBRAINSKOTLIN-2393744", v010)
    suppress("Is a Gradle build dependency only and is not exposed in the release build. No Update available.")
}
