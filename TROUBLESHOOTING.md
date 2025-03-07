# Troubleshooting

## Syntax Highlighting in IntelliJ IDE

**Issue:** IntelliJ IDE does not show syntax highlighting for `.vl.kts` files.

1. Check IntelliJ's _Externel Libraries_, your project should have `dev.vulnlog:dsl:<version>` on the
   `runtimeClasspath`. If there is no DSL Jar there, check if you have configured the correct Gradle plugin and version.
2. Check whether _Settings > Languages & Frameworks > Kotlin > Kotlin Scripting_ contains an entry for `.vl.kts`files.
   Make sure the entry has a higher priority (is above) than the Kotlin script entry `.kts`.
