---
title: CLI
description: 
---

# {{ title }}

```
$ ./bin/vl --help
Usage: main [<options>] <vulnlogfile> <command> [<args>]...

  CLI application to parse Vulnlog files.

Options:
  --vuln=<text>...    Filter to specific vulnerabilities
  --branch=<text>...  Filter to specific branches
  -v, --version       Show version number and exit.
  -h, --help          Show this message and exit

Arguments:
  <vulnlogfile>  The Vulnlog definition files to read.

Commands:
  report  Generate a Vulnlog report files.
```
