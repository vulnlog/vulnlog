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

- The affected version (`vulnlog --version`) and platform.
- A description of the issue and its impact.
- Steps to reproduce, ideally with a minimal Vulnlog file or command.

## Response

We aim to acknowledge a report within a few business days and to provide a triage outcome
and remediation timeline after the initial assessment. We will coordinate disclosure with
you and credit your report unless you prefer to remain anonymous.

## Vulnerability status (VEX)

Vulnlog publishes an [OpenVEX](https://github.com/openvex/spec) document for each release
artifact. It states which reported vulnerabilities affect which release, and is generated
by Vulnlog from [`vulnlog.yaml`](vulnlog.yaml).

| Artifact                                  | OpenVEX document                                           |
|-------------------------------------------|------------------------------------------------------------|
| CLI                                       | https://vulnlog.dev/vex/vulnlog-cli.openvex.json           |
| Container image `ghcr.io/vulnlog/vulnlog` | https://vulnlog.dev/vex/vulnlog-container.openvex.json     |
| Gradle plugin `dev.vulnlog.plugin`        | https://vulnlog.dev/vex/vulnlog-gradle-plugin.openvex.json |

Each document covers all published releases and keeps its `@id`; its `version` increases
whenever a statement changes. The same files are committed in [`.vex/`](.vex). Pass a
document to your scanner to drop the findings that do not affect a release, for example
`trivy image --vex vulnlog-container.openvex.json ghcr.io/vulnlog/vulnlog:0.18.0`.
