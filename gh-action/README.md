# Vulnlog GitHub Action

Run [Vulnlog](https://vulnlog.dev) in your GitHub Actions workflows. The action downloads the Vulnlog CLI and generates scanner suppression files and an HTML vulnerability report from your Vulnlog file, all in a single step.

## Usage

With a `vulnlog.yaml` in the repository root, no configuration is needed:

```yaml
- name: Run Vulnlog
  uses: vulnlog/vulnlog/gh-action@v0.17.0
```

This generates suppression files for every reporter in your Vulnlog file and writes an HTML report to `vulnlog-report.html`. Suppression files that would otherwise be missing, for example when no vulnerability is suppressed yet, are created empty so scanners always find them.

A typical dynamic suppression setup generates the files right before the scanner runs:

```yaml
- name: Run Vulnlog
  uses: vulnlog/vulnlog/gh-action@v0.17.0
  with:
    reporter: trivy
    ensure-files: .trivyignore.yaml

- name: Run Trivy scan
  uses: aquasecurity/trivy-action@0.36.0
  with:
    scan-type: fs
    scan-ref: .
    trivyignores: .trivyignore.yaml
```

## Inputs

| Input | Default | Description |
| ----- | ------- | ----------- |
| `version` | current release | Vulnlog CLI version to download, matching a [released version](https://github.com/vulnlog/vulnlog/releases). |
| `file` | `vulnlog.yaml` | Path to the Vulnlog file. |
| `suppress` | `true` | Generate suppression files. |
| `report` | `true` | Generate the HTML report. |
| `output-dir` | `.` | Directory the suppression files are written to. |
| `report-output` | `vulnlog-report.html` | File path the HTML report is written to. |
| `format` | `auto` | Suppression file format. `auto` uses each reporter's native format, `generic` forces the generic Vulnlog JSON format. |
| `release` | | Only include vulnerabilities up to and including this release. |
| `tags` | | Comma-separated list of tags to filter on. |
| `reporter` | | Only include reports from this reporter. |
| `ensure-files` | `.snyk .trivyignore.yaml` | Space-separated file names created empty in `output-dir` when suppression generates no file for them. Set to an empty string to disable. |

## Outputs

| Output | Description |
| ------ | ----------- |
| `report-file` | Path of the generated HTML report, ready for `actions/upload-artifact`. |

## Requirements

The action runs on GitHub-hosted `ubuntu`, `macos`, and `windows` runners, and on self-hosted runners with matching platforms (`linux/amd64`, `macos/aarch64`, `windows/amd64`). Docker is not required.

The action is available from Vulnlog 0.17.0 on. Pin it to a release tag of this repository; the tag also selects the default CLI version.
