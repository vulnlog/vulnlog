# Fmt Command

`vulnlog fmt` rewrites a Vulnlog YAML file in canonical style. It is a formatter, not a linter or validator:

| Command    | Question                          | Output                          | Modifies file?            |
|------------|-----------------------------------|---------------------------------|---------------------------|
| `validate` | Is the file legal?                | Errors                          | No                        |
| `lint`     | Is the file sensible? (future)    | Warnings / findings             | Optionally with `--fix`   |
| `fmt`      | Is the file canonically styled?   | Rewritten file or check status  | Yes (in-place or stdout)  |

The three concerns are orthogonal. `fmt` only changes whitespace, quoting, sequence shape, and field order. It never changes semantics. Running `fmt` twice on the same file is a no-op.

## Invocation

```
vulnlog fmt <file>            # rewrite in place
vulnlog fmt --stdout <file>   # write formatted result to stdout
vulnlog fmt --check <file>    # exit 0 if already formatted, 1 otherwise
vulnlog fmt -                 # read stdin, write stdout
```

## Output style

- Block sequences for two or more elements, dash on its own line.
- Flow sequences for exactly one scalar element: `key: [ "value" ]`. Single-element sequences whose item is a nested mapping stay block.
- Minimal quoting of scalars (`YAMLWriteFeature.MINIMIZE_QUOTES`).
- Field order defined by `VulnerabilityEntryDto` property order.
- No leading `---` document marker.

Comments and blank lines from the input are not preserved. This keeps the implementation on the existing Jackson 3 YAML stack with no new dependencies. Files intended for `fmt` should not rely on inline comments for important context.

## Implementation

`fmt` reuses existing machinery; it adds no new YAML coupling.

### Reused components

| Concern              | Existing component                                                                  |
|----------------------|-------------------------------------------------------------------------------------|
| Parse YAML → domain  | `YamlParser` + `V1Mapper.toDomain`                                                  |
| Domain → DTO         | `V1Mapper.toDto`                                                                    |
| DTO → YAML string    | `YamlWriter.write` (`modules/lib/src/main/kotlin/dev/vulnlog/lib/parse/YamlWriter.kt`) |
| YAML mapper          | `createYamlMapper` (`modules/lib/src/main/kotlin/dev/vulnlog/lib/parse/YamlMapperFactory.kt`) |
| CLI framework        | Clikt 5.1                                                                            |

### New code

`FmtCommand` (`modules/cli-app/src/main/kotlin/dev/vulnlog/cli/shell/FmtCommand.kt`): Clikt command class modeled on `InitCommand` and `ValidateCommand` in the same package.

```
val input = readInput(file)
val parsed = parseInputOrFail(input)
val formatted = YamlWriter.write(parsed, mapper)
when {
    check  -> exitWithDiffStatus(input, formatted)
    stdout -> echo(formatted)
    else   -> Files.writeString(file, formatted)
}
```

Reuses `parseInputOrFail` from `modules/lib/src/main/kotlin/dev/vulnlog/lib/shell/ParseFile.kt` for consistent error handling.

`compactSingleElementSequences(yaml: String): String` (new helper alongside `YamlWriter.write`): pure text post-pass on the emitted YAML string. Jackson 3 has no per-sequence flow/block toggle (`YAMLWriteFeature` exposes only `INDENT_ARRAYS`, `MINIMIZE_QUOTES`, `LITERAL_BLOCK_STYLE`, etc.), so this rule must be applied after serialization.

Transformation:

```
<indent>key:                           <indent>key: [<scalar>]
<indent>  - <scalar>            ──►
<next line at ≤indent or EOF>          <next line at ≤indent or EOF>
```

Detection rules (line-based, no YAML re-parse):

1. A line matches `^(\s*)([A-Za-z_][A-Za-z0-9_]*):\s*$` (capture indent and key).
2. The next line matches `^<indent>  - (.+)$` where the captured value does not end with `:` and does not start with `|` or `>`.
3. The line after either does not exist or has leading-whitespace ≤ length of `<indent>`.

When all three hold, lines N and N+1 collapse to `<indent>key: [<scalar>]`. The scalar text is moved verbatim — Jackson's quoting is preserved. PURLs containing colons are safe because the detector inspects only the key line's trailing colon, not the value contents. Single-element object lists (e.g., a single `reports:` entry) are not collapsed because line N+1 ends with `:` (`- reporter:`), failing rule 2.

Apply inside `YamlWriter.write` as the final step. Apply the same compaction inside `serializeEntryYaml` (`modules/lib/src/main/kotlin/dev/vulnlog/lib/core/Vulnerability.kt`) so `add` and `copy` emit matches `fmt` emit — this is the change that removes format churn on `add` updates.

### Edits to existing files

- `modules/cli-app/src/main/kotlin/dev/vulnlog/cli/shell/Main.kt`: append `.subcommands(FmtCommand())` to the existing chain.
- `modules/lib/src/main/kotlin/dev/vulnlog/lib/parse/YamlMapperFactory.kt`: enable `YAMLWriteFeature.MINIMIZE_QUOTES`; disable `WRITE_DOC_START_MARKER`. Global change; lock the resulting style with golden-file tests in the same commit.
- `modules/lib/src/main/kotlin/dev/vulnlog/lib/parse/YamlWriter.kt`: call `compactSingleElementSequences` as the final step.
- `modules/lib/src/main/kotlin/dev/vulnlog/lib/core/Vulnerability.kt`: same call in `serializeEntryYaml`.

### Tests

- `FmtCommandTest`: in-place rewrite, `--stdout`, `--check` clean (exit 0), `--check` dirty (exit 1), stdin/stdout (`-`).
- `YamlWriterTest`: golden-file round-trip covering every optional field (aliases, multiple reports with `vuln_ids` and `suppress`, resolution, every verdict variant). Locks the canonical style.
- `compactSingleElementSequencesTest`:
    - one-scalar list → compacted (`releases: [ "0.11.0" ]`)
    - two-scalar list → unchanged block style
    - one-object list (single `reports:` entry) → unchanged block style
    - nested key after the list → boundary detection works
    - empty list (`[]`) → unchanged
    - compacted output re-parses to the same domain object

## Coupling and tradeoffs

- No new dependency. Stays on Jackson 3.1.1.
- No new abstraction. Reuses `YamlWriter.write`, `V1Mapper`, `createYamlMapper`, Clikt.
- The implicit constraint that every domain field must round-trip cleanly through the DTO becomes explicit and testable.
- Choosing Jackson means comments and inline ordering are not preserved. A future shift to comment-preserving format (CST round-trip) would require a different YAML library and a rewrite of this command.

## End-to-end verification

1. `./gradlew build` — all tests pass.
2. Round-trip test: `parse(write(parse(input))) == parse(input)` for a representative `vulnlog.yaml` covering every optional field. Failing this means a domain field is silently dropped on emit.
3. Manual against the repo's own `vulnlog.yaml`:
    - `vulnlog fmt --check vulnlog.yaml` on a known-canonical file → exit 0.
    - Hand-edit `aliases: [ "X" ]` to multi-line block form and re-run `--check` → exit 1.
    - `vulnlog fmt vulnlog.yaml` → file rewritten; second run is a no-op (idempotency).
4. Cross-check with `add`: after `vulnlog fmt`, run an `add` update — the resulting diff should touch only the keys the user supplied.
