val v100 = version("1.0.0")
val v010 = version("0.1.0")

val r1 = branch("release 1", v100, v010)

cve("CVE-2019-10782") {
    owasp(v010)
    suppress(v010, rationale = "This is a test suppress for demonstration purpose")
}

cve("CVE-2019-9658") {
    owasp(v010)
    suppress(v010, rationale = "This is a test suppress for demonstration purpose")
}

cve("CVE-2023-6378") {
    owasp(v010)
    suppress(v010, rationale = "This is a test suppress for demonstration purpose")
}
