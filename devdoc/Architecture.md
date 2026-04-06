# Architecture

The project follows a data-oriented programming (DOP) approach.

- [`core`](../next/cli-app/src/main/kotlin/dev/vulnlog/cli/core) Contains pure functions. This code has a focus on
  clarity and should be free of side effects.
- [`model`](../next/cli-app/src/main/kotlin/dev/vulnlog/cli/model) Contains data classes. These classes have no
  dependencies outside the module.
- [`parse`](../next/cli-app/src/main/kotlin/dev/vulnlog/cli/parse) Contains the parsing, the DTOs and the mappings. It
  acts as a bridge between I/O operations and the `core` logic.
- [`result`](../next/cli-app/src/main/kotlin/dev/vulnlog/cli/result) Contains result types.
- [`shell`](../next/cli-app/src/main/kotlin/dev/vulnlog/cli/shell) Contains the CLI commands and is responsible for I/O
  and exit codes.
