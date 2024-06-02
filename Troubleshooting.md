# Troubleshooting

## Syntax Highlighting in IntelliJ IDE

**Q:** IntelliJ IDE does not support syntax highlighting for `.vulnlog.kts` files.

**A:** Check whether Settings > Languages & Frameworks > Kotlin > Kotlin Scripting contains an entry for `.vulnlog.kts`
files. Make sure the entry has a higher priority (is above) than the Kotlin script entry `.kts`.
