# Logging

How Vulnlog communicates on the terminal, and how to decide whether a new message belongs in the code and at what level.
The CLI flags are `-v`, `-vv` and `-q`. The Gradle plugin emits the same events through the Gradle logger (`--info`
shows verbose, `--debug` shows debug).

## Channels

- stdout carries command output only (YAML, HTML, suppression content). Redirections like
  `vulnlog suppress -o - > .trivyignore.yaml` must stay safe at any verbosity.
- Everything else is secondary: status lines print to stdout but are suppressible with `-q`. warnings, errors and
  diagnostics go to stderr.
- There is no log file. Users capture diagnostics by redirecting stderr.

## Levels

| Level   | Shown                  | Audience   | Purpose                                                                             |
|---------|------------------------|------------|-------------------------------------------------------------------------------------|
| status  | default, off with `-q` | user       | One line per completed user-visible action.                                         |
| warning | always                 | user       | Surprising but non-fatal.                                                           |
| error   | always                 | user       | The operation failed; one actionable line.                                          |
| verbose | `-v`                   | user       | What the tool did with the user's data.                                             |
| debug   | `-vv`                  | maintainer | Internal decisions for bug hunting; also enables stack traces on unexpected errors. |

## Should this log at all, and at what level?

The first match wins.

1. Can the user infer it from the command output or an existing message? Do not log.
2. Did the command create or change something the user asked for? Status.
3. Is something surprising but not fatal (data loss on rewrite, deprecated usage)? Warning.
4. Did a step fail? Error, once, with the reason and the file or id involved.
5. Does it explain what happened to the user's data: a file parsed, a filter resolved, a file written, an entry skipped
   and why? Verbose.
6. Does it only help a maintainer diagnose: per-entry decisions, intermediate counts, why the code chose a branch?
   Debug.
7. Otherwise do not log. The goal is to not overload the verbosity features with unrelevant information.

Two hard rules:

- Every skip or drop decision and every file write must be observable at `-v`.
- Never log whole file contents.

## Typical messages

Verbose answers "why is X (not) in the output?":

```
verbose: parsed vulnlog.yaml: schema version 1, releases: 2, tags: 1, vulnerabilities: 4
verbose: release filter expanded to releases: 1.2.0, 1.2.1
verbose: validated vulnlog.yaml: 2 warning(s)
verbose: skipped CVE-2026-1234 for .snyk: the snyk format requires SNYK ids
verbose: wrote .trivyignore.yaml: trivy format, 3 entries
```

Debug answers "what exactly did the code decide?":

```
debug: included CVE-2026-1234 for reporter trivy (expires 2026-09-01)
debug: collected 12 report entries, merged to 9
debug: [non-canonical-array-style] vulnerabilities[0].releases: Line 12: canonical style for this list is a flow array, e.g. key: [value].
```

## Architecture

- `core` never logs. It returns data that carries the facts worth reporting, for example `SuppressionExclusion` or
  `SuppressionCollectionResult`.
- Shared `render*` functions in `modules/lib` turn that data into the exact message text, so the CLI and the Gradle
  plugin cannot drift apart.
- Shells emit through `DiagnosticSink` (`modules/lib/.../shell/Diagnostics.kt`). The CLI sink filters by `Verbosity` and
  prefixes `verbose:`/`debug:` on stderr; the Gradle sink forwards to `logger.info`/`logger.debug`.
- If producing a debug message costs real work, guard the call site: `verbosity.enables(DiagnosticLevel.DEBUG)` in the
  CLI, `logger.isDebugEnabled` in Gradle.

## Message style

- Diagnostics: lowercase, verb-first, past tense (`parsed x`, `wrote y`, `skipped z: reason`), one line per event, no
  trailing period, ASCII only.
- Include the identifier the user would grep for: file name, vulnerability id, reporter.
- Status lines keep the existing human style (`Formatted: ...`, `Validation OK`).
