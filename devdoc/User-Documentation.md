# User Documentation Guidelines

This document defines what good user documentation for Vulnlog looks like: who it
serves, what it contains, how it is written, how deep it goes, and how it is
structured in Antora. It is the yardstick for reviewing and reworking the content
under `docs/`.

Scope: user-facing documentation only. Contributor and maintainer documentation
stays in `devdoc/` and follows its own conventions.

## Audience and personas

Vulnlog documentation serves four personas with equal weight. No persona is the
"default reader"; the landing page routes each to their own entry path.

| Persona             | Arrives with                                                           | Wants                                                                               | Done when                                                                                       |
|---------------------|------------------------------------------------------------------------|-------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| Triaging developer  | New findings from an SCA scanner, often as a failing build             | Analyse the finding, record a verdict, and unblock the build, fast                  | The finding is triaged in `vulnlog.yaml`, the suppression file is generated, the build is green |
| Releasing developer | An upcoming bugfix release                                             | Know which entries affect the release and which dependency updates are due          | The update list for the release is clear, resolutions are recorded once shipped                 |
| Product manager     | The question "how badly are we affected, and by what?"                 | An impact and severity overview per release, without editing YAML or running builds | States impact and open risks from the latest report                                             |
| Responder           | A customer or user whose scanner flags a vulnerability in your product | Find the recorded verdict, justification, and analysis, and quote them confidently  | Answers the inquiry from the Vulnlog file without redoing the triage                            |

Personas are jobs, not job titles. In an open source project on GitHub, one
maintainer often covers all four in a single afternoon; in a company they spread
across development, product management, and support. The responder is a support
engineer answering a customer, or a maintainer answering an issue from a
downstream user whose scanner flagged the project. Downstream users and
customers themselves are not a documentation persona; the responder serves them,
for example by sharing a generated report.

Assume the two developer personas know their build tool, their scanner, and
basic git. Assume the product manager and the responder can read a generated
report and navigate a file on GitHub, but do not edit YAML or run builds. Do not
assume anyone knows vulnerability management vocabulary (VEX, PURL, triage);
introduce such terms on first use with one short sentence and a link to the
concept or reference page that owns the definition.

## What users want to know

Each persona brings a set of questions. Every question must have exactly one page
that answers it, and the landing page or navigation must make that page findable
in at most two clicks.

Evaluating ("should we use this?"):

- What is Vulnlog and what problem does it solve?
- Why record decisions in a git-tracked YAML file instead of `.trivyignore`,
  inline suppressions, or a vulnerability management platform?
- Which scanners and formats are supported today?
- What does adopting it cost (setup effort, workflow changes)?

Adopting ("how do I start?"):

- How do I install the CLI or apply the Gradle plugin?
- How do I create my first `vulnlog.yaml`?
- How do I record my first decision and suppress my first finding?
- How do I wire validation and suppression into CI?

Triaging ("what do I do with this finding?"):

- The scanner flagged a new vulnerability: what now?
- How do I record a verdict, an analysis, and a resolution?
- How do I handle a vulnerability that affects several releases or several files?
- How do I keep the file tidy (`fmt`) and correct (`validate`)?

Planning updates ("what do we ship next?"):

- Which entries affect the upcoming release, and what fixes them?
- Which entries are still open, with no resolution recorded?
- How do I filter entries by release or tag?

Assessing impact ("how affected are we?"):

- How severe is the impact on a given release, and from which vulnerabilities?
- How do I generate a report and what does it show?
- How do I trace who decided what, and when?
- How do I find decisions that need re-review?

Answering inquiries ("what do we tell them?"):

- A customer or user reports a CVE their scanner found: what is our recorded
  position?
- How do I find an entry by ID, alias, or package?
- What do verdict and justification mean when I quote them in an answer?
- What can I share: the report, the file, or both?

Integrating ("how does it behave?"):

- What exit codes do the CLI and the Gradle tasks return, and what do they mean
  for a pipeline?
- What are all the fields, flags, tasks, and defaults?
- How do schema versions evolve and how do I upgrade?

Do not document what Vulnlog deliberately excludes: CVSS scoring, CWE taxonomy,
advisory contents, and general vulnerability management theory. Where the
exclusion itself surprises users (why is there no `cvss` field?), explain the
design decision once on a concept page.

## Language and tone

The voice is formal but friendly: precise and technical, never stiff, never
marketing. The reader is a professional in the middle of a task; respect their
time.

- Address the reader as "you". Refer to Vulnlog as "Vulnlog", to the command as
  `vulnlog`, and to the file as `vulnlog.yaml`.
- Write instructions in the imperative: "Run `vulnlog validate vulnlog.yaml`."
- Use present tense and active voice: "The command writes the report to
  `vulnlog-report.html`", not "the report will be written".
- Keep sentences short. One idea per sentence, one topic per paragraph.
- State facts without hedging. If behavior depends on a condition, name the
  condition instead of writing "may" or "should".
- No exclamation marks, no humor that depends on cultural context, no
  superlatives ("powerful", "seamless", "simply").
- ASCII only. No em dashes and no ellipses; a sentence is never split with "--".
- Every code snippet is complete and copy-paste runnable against the documented
  version. Show the expected output whenever a command prints something the
  reader must interpret.

### Terminology

Define each term once (on the concept or reference page that owns it) and use it
identically everywhere. Never rotate synonyms for elegance.

| Term             | Meaning                                                                  | Do not say                             |
|------------------|--------------------------------------------------------------------------|----------------------------------------|
| Vulnlog file     | A YAML document following the Vulnlog schema, typically `vulnlog.yaml`   | log file, database                     |
| entry            | One item in the `vulnerabilities` list                                   | record, finding, issue                 |
| verdict          | The triage decision for an entry, for example `not affected`             | status, state, decision                |
| justification    | The machine-readable VEX reason for a `not affected` verdict             | reason, excuse                         |
| analysis         | The free-text rationale behind a verdict                                 | comment, note                          |
| reporter         | A scanner or source that reported the vulnerability, for example `trivy` | scanner (when meaning the field), tool |
| report           | The fact that a reporter flagged an entry at a date                      | finding                                |
| release          | A version of your product tracked in the Vulnlog file                    | version (when meaning the field)       |
| tag              | A user-defined label on entries or defined at file level                 | label, category                        |
| suppression file | A generated file that tells a scanner which findings to ignore           | ignore file, exclusion                 |
| resolution       | How and when an entry was resolved                                       | fix                                    |

## Detail level

Depth is layered by category. A page never mixes depths: a guide does not explain
theory, a concept page does not list flags, a reference page does not teach.

| Category    | Depth                                                                                             | Excludes                                            |
|-------------|---------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| Get started | Shallow happy path. One scanner, one file, defaults everywhere. Success in minutes.               | Options, edge cases, alternatives                   |
| Guides      | One task deep. All realistic variants of that one task, prerequisites stated up front.            | Theory, exhaustive option lists                     |
| Concepts    | As deep as the mental model requires. Explains why, trade-offs, and design decisions.             | Step-by-step instructions, implementation internals |
| Reference   | Exhaustive contract. Every field, flag, task, property, exit code, message category, and default. | Tutorials, motivation                               |

The reference is part of Vulnlog's public contract. A CLI flag, YAML field,
Gradle property, or exit code exists in the docs if and only if it exists in the
released version, with its default and its constraints. "See `--help`" or "see
the JSON schema" is never the answer; the docs restate the full contract in
readable form (and link to the schema as the machine-readable source).

Internals (module layout, DOP architecture, DTO mapping) never appear in user
documentation. If a user-visible behavior needs explaining, explain the behavior,
not the code.

## Structure

The documentation follows a Diataxis-informed hybrid: the top level is organized
by the reader's journey (learn, do, understand, look up), and the reference
level is organized by component (YAML format, CLI, Gradle plugin). New
components, such as a GitHub Action, extend the docs with one reference section
and one or two guides; they never get a parallel documentation tree of their own.

Rationale: readers arrive with a task, not with a component in mind. A developer
suppressing a finding does not care whether the CLI or the Gradle plugin does the
work; the guide decides that for them and links to the component reference for
details.

### Antora mapping

- One Antora component `vulnlog`, versioned with the product minor version, as
  today. The `vulnlog-version` attribute pins snippet versions.
- One module (`ROOT`) with one `nav.adoc`; categories are directories under
  `pages/` (`get-started/`, `guides/`, `concepts/`, `reference/`). Split into
  separate Antora modules only when a category grows past roughly fifteen pages.
- Shared fragments live in `partials/`: install instructions, the standard
  prerequisite block, warnings that appear on several pages.
- Runnable YAML and build snippets live in `examples/` and are included with
  `include::`, so they can be validated by tooling instead of drifting inside
  prose.
- Navigation is at most two levels deep below the top-level categories. If a
  third level seems necessary, the pages are cut wrong.

## Target navigation tree

The nav a reader should see. Page names are working titles; guide titles always
start with a verb.

```
Vulnlog
  index (landing: what Vulnlog is + four persona routes)
Get started
  What is Vulnlog?          (elevator pitch, how it compares to ignore files)
  Install                   (CLI binary, Docker, Gradle plugin; one page, tabs)
  Quickstart: triage your first finding        (triaging developer)
  Quickstart: find what affects your release   (releasing developer)
  Quickstart: read the impact report           (product manager)
  Quickstart: answer a vulnerability inquiry   (responder)
Guides
  Triage a new scanner finding
  Generate suppression files for your scanner
  Record a resolution
  Work with releases and tags
  Find what affects a release
  Answer a vulnerability inquiry
  Validate and format Vulnlog files
  Generate reports
  Run Vulnlog in CI pipelines
  Migrate from scanner ignore files
Concepts
  Why decisions live in git
  The Vulnlog data model     (entries, verdicts, reports, releases, tags)
  Vulnlog and your scanner   (reporters, suppression formats, the generic format)
  What Vulnlog leaves out    (no CVSS, no advisory data, enrichment at generation time)
Reference
  The Vulnlog file format    (every field, per schema version; links to JSON schema)
  CLI                        (one page per command: init, validate, fmt, suppress,
                              report, modify add, modify copy; plus global options
                              and filters)
  Gradle plugin              (extension properties, one section or page per task)
  Exit codes and messages    (exit code table, message categories, color behavior)
Troubleshooting
  Troubleshooting            (symptom-keyed sections with stable anchors)
Upgrade
  Upgrade notes              (schema version changes, breaking CLI changes)
```

The four quickstarts share the same example vulnerability, so a reader who
follows a second quickstart recognizes the scenario immediately. The two
developer quickstarts share the install partial; the product manager and
responder quickstarts start from an existing example file and need no
installation.

## Page templates

Every page belongs to exactly one category and follows that category's template.
Section order is fixed; optional sections are dropped, never reordered.

### Landing page

1. One-paragraph statement of what Vulnlog is and the problem it solves.
2. Four persona routes ("Your scanner flagged a finding", "You prepare the
   next release", "You need the impact picture", "Someone asks about a
   vulnerability"), each one sentence plus a link.
3. Links to Reference and Concepts for returning readers.

### Quickstart

1. Goal and time promise ("In about ten minutes you will ...").
2. Prerequisites (tools, versions, and the starting situation, for example a
   scanner finding to triage or an existing Vulnlog file to read).
3. Numbered steps. Each step: one action, the exact command or snippet, the
   expected output or resulting file content.
4. "What you did" recap in two or three sentences, linking each new term to its
   concept or reference page.
5. Next steps (two or three links into Guides).

### Guide (how-to)

1. Title starting with a verb; first paragraph states the goal and the end state.
2. Prerequisites, including links to whatever quickstart or guide comes before.
3. Steps for the main path, then variants as separate subsections ("If you use
   the Gradle plugin", "If you track multiple releases").
4. Verification: how the reader confirms it worked (command plus expected
   output, or expected exit code).
5. Related links (sibling guides, owning reference pages).

### Concept page

1. Context: the question this page answers, in the reader's words.
2. Explanation, from familiar to new. Diagrams where structure matters.
3. Consequences: what this means for daily work.
4. Links to the guides and reference pages that put the concept to work.

### Reference page

1. Synopsis (command line, task invocation, or top-level YAML shape).
2. One-paragraph description of purpose and behavior.
3. Complete table of fields, options, or properties: name, type, required or
   default, constraints, description.
4. Exit codes or failure modes, where applicable.
5. Examples: minimal first, then one realistic example.
6. Related entries (the guide that uses this, adjacent commands or fields).

## Cross-cutting rules

- One page, one purpose. If a page answers questions from two categories, split
  it.
- Guides link to reference pages; they never restate option tables or field
  lists. Reference pages link back to the guide that shows the option in
  context.
- Every snippet names or pins the version it was written against via the
  `vulnlog-version` attribute, and every example file lives in `examples/` so
  it can be checked by CI (at minimum: `vulnlog validate` on every example
  Vulnlog file).
- Troubleshooting content lives on the central troubleshooting page, keyed by
  the symptom the user sees (the exact error message), with a stable anchor per
  symptom. Guides link to anchors instead of embedding failure discussions.
- Screenshots only where the artifact is visual (the HTML report). Everything
  else is text, so it stays diffable and current.
- A change to a CLI flag, YAML field, Gradle property, exit code, or output
  message is complete only when the owning reference page changes in the same
  pull request.
- New features enter the docs bottom-up: reference first (the contract), then a
  guide if the feature is a task users perform, then a concept only if the
  feature changes the mental model.
