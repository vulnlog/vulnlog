# User documentation guidelines

These rules apply to everything under `docs/`. To goal is to get a user-oriented and user first documentation helping
Vulnlog users to learn about the format and tooling and so they can achieve their goals.

## Structure

- Follow [Diataxis](https://diataxis.fr/). Every page is a tutorial, a how-to guide, a reference page, or an
  explanation. Never a mix.
- Tutorials teach by doing. How-to guides solve one task. Reference describes the product. Explanation says why.
- Start how-to titles with a verb, for example "Generate suppression files".
- Say each thing once. Link to the page that owns it.

## Content

- Write for people who use Vulnlog: developers who analyse scanner findings, and the people who rely on the results.
- Describe what users see and do. Leave out internals such as code structure, class names, and implementation steps.
- Document the features of the release the documentation ships with. Docs for the next release are written on `main`
  (see `devdoc/Releasing.md`).
- Describe the current behaviour only. Leave out history: renamed options, deprecated values, and how earlier versions
  behaved.
- Explain terms such as VEX and PURL where they first appear on a page.
- Use the same example everywhere: the Acme Shop project in `docs/modules/ROOT/examples/acme-shop.vl.yaml`.

## Style

Follow the [Microsoft Writing Style Guide](https://learn.microsoft.com/en-us/style-guide/welcome/). In particular:

- Get to the point. Put what the reader needs first.
- Address the reader as "you". Write instructions in the imperative.
- Use active voice and present tense.
- Keep sentences and paragraphs short.
- Use contractions such as "it's" and "don't".
- Use sentence-style capitalisation in titles and headings.
- Don't use words such as "simply", "easy", "powerful", or "seamless".

## House rules

These override the Microsoft Writing Style Guide where they differ.

- Use British English: analyse, behaviour, organisation, licence (noun). Keep commands, field names, and values as they
  are, for example `analyzed_at`.
- Use ASCII characters only. No em dashes, en dashes, curly quotes, or ellipsis characters.
- Don't use semicolons. Split the sentence instead.
- Don't use exclamation marks.
- Put commands, file names, field names, and values in backticks.
- Do not use **bold** text, or only if there is no better way.

## Terminology

Use these terms consistently.

| Use              | For                                                                     | Not                     |
|------------------|-------------------------------------------------------------------------|-------------------------|
| Vulnlog file     | A YAML file in the Vulnlog format, for example `vulnlog.yaml`           | log, database           |
| finding          | A vulnerability a scanner reports                                       | alert, hit              |
| entry            | A vulnerability recorded in the Vulnlog file                            | record, issue           |
| verdict          | `affected` or `not affected`                                            | status, decision        |
| analysis         | The reasoning behind a verdict                                          | comment, note, decision |
| state            | `open`, `under investigation`, `accepted`, `resolved`, `not applicable` | status                  |
| resolution       | The release that fixes a vulnerability (the field)                      | fix (for the field)     |
| suppression file | A file Vulnlog generates so a scanner ignores a finding                 | ignore file             |
| PURL             | A package URL, for example `pkg:npm/lodash@4.17.20`                     | purl (outside code)     |
| canonical style  | The layout `vulnlog fmt` writes                                         | canonical format        |

## Conventions

- Write field names, values, verdicts, and states in backticks: `not affected`, `open`. When a page describes the impact
  report, use the label the report shows in italics, and give the state at the first mention: _Investigating_ (the state
  `under investigation`).
- Explain OpenVEX at its first mention on a page: "OpenVEX documents, which tell your customers' tools whether a release
  is affected by a vulnerability". Explain PURL the same way: "package URL (PURL)".
- Use the title of the target page as link text.
- Put "See" links in their own sentence: "Run `vulnlog fmt`. See xref:...[Validate and format files]."
- Tutorials show a command in a `[source,bash]` block and its output in a separate `[source]` block. How-to guides and
  reference pages use one `[source,console]` block with a `$` prompt.
- Gradle examples use assignment: `strict = true`, not `strict.set(true)`.
