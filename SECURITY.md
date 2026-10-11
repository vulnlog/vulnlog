# Security Policy

## Supported versions

Vulnlog is pre-1.0 and under active development. Security fixes are released against the
latest published version. Older releases do not receive backported fixes; upgrade to the
latest release to stay supported.

## Reporting a vulnerability

Please report security issues privately. Do not open a public GitHub issue for a suspected
vulnerability.

Use GitHub private vulnerability reporting: go to the
[Security advisories](https://github.com/vulnlog/vulnlog/security/advisories/new) page of
the repository and click "Report a vulnerability". This keeps the report private until a fix
is available.

Include as much detail as you can:

- The affected version (`vulnlog --version`) and how you run it: CLI, container image or
  Gradle plugin.
- A description of the issue and its impact.
- Steps to reproduce, ideally with a minimal Vulnlog file or command.

## Response

We aim to acknowledge a report within a few business days and to provide a triage outcome
and remediation timeline after the initial assessment. We will coordinate disclosure with
you and credit your report unless you prefer to remain anonymous.

## Known vulnerabilities in dependencies

Before you report a scanner finding in one of Vulnlog's dependencies, check
the [OpenVEX](https://github.com/openvex/spec) document of the artifact. It states for each release whether a known
vulnerability affects
it: [CLI](https://vulnlog.dev/vex/vulnlog-cli.openvex.json), [container image](https://vulnlog.dev/vex/vulnlog-container.openvex.json)
and [Gradle plugin](https://vulnlog.dev/vex/vulnlog-gradle-plugin.openvex.json). Pass it to your scanner to hide the
findings that do not apply, for example
`trivy image --vex vulnlog-container.openvex.json ghcr.io/vulnlog/vulnlog:<version>`.

## SBOMs and release verification

Each [GitHub release](https://github.com/vulnlog/vulnlog/releases) attaches [CycloneDX](https://cyclonedx.org) SBOMs
(`*.cdx.json`) for the JVM distribution, the native binaries, the container image and the Gradle plugin, and a
`SHA256SUMS` file.

The SBOMs state what they do not know. A component producer of `unknown` means the component's package metadata names
none. Hashes are missing where there is no file to hash: metadata-only Maven modules and the Alpine packages inside the
image. The attestations bind each release archive to its SBOM. The GraalVM components of the native and container
SBOMs carry no license, because Native Image records none. Ask about a gap or report a wrong SBOM in a GitHub issue;
the next release corrects it.

The release workflow signs build provenance and
SBOM [attestations](https://docs.github.com/en/actions/security-guides/using-artifact-attestations-to-establish-provenance-for-builds)
for the release files, the container image and the Gradle plugin jar. Verify that an artifact was built by Vulnlog's
release workflow with the GitHub CLI:

```sh
gh attestation verify vulnlog-linux-amd64.zip --repo vulnlog/vulnlog
gh attestation verify oci://ghcr.io/vulnlog/vulnlog:<version> --repo vulnlog/vulnlog
```

The SBOMs are signed the same way. Verify an SBOM file from the release, or the SBOM attested to the container image:

```sh
gh attestation verify vulnlog-<version>.cdx.json --repo vulnlog/vulnlog
gh attestation verify oci://ghcr.io/vulnlog/vulnlog:<version> --repo vulnlog/vulnlog --predicate-type https://cyclonedx.org/bom
```
