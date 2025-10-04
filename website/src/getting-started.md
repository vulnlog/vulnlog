---
title: Getting Started
description: 
layout: base.liquid
---

# {{ title }}

The easiest way to introduce Vulnlog to your software project is by using
the [Gradle Vulnlog Plugin](https://plugins.gradle.org/plugin/dev.vulnlog.dslplugin).

Let's assume a simple project structure like this:

```terminaloutput
.
├── app
│   ├── build.gradle.kts
│   └── src
│       ├── main
│       │   ├── kotlin
│       │   │   └── org
│       │   │       └── example
│       │   │           └── App.kt
│       │   └── resources
└── settings.gradle.kts
```

Let's set up Vulnlog in the project.

## Setup Vulnlog

1. Add the two Vulnlog files `definitions.vl.kts` and `app.vl.kts`. You can choose the name of the
   latter as long as the `.vl.kts` suffix remains.
    ```terminaloutput
    touch app/definitions.vl.kts app/app.vl.kts
    ```

2. In `app/definitions.vl.kts` define a release branch and a reporter as follows:
    ```kotlin
    releases {
        branch("My First Release Branch") {
            release("0.1.0")
        }
    }
    
    reporters {
        reporter("OWASP Dependency Check")
    }
    ```

3. Configure the Gradle Vulnlog plugin in the build file `app/build.gradle.kts` and add a `vulnlog`
   configuration block:
    ```kotlin
    plugins { 
        id("dev.vulnlog.dslplugin") version "$version"
    }
   
    vulnlog {
       definitionsFile.set(layout.projectDirectory.file("definitions.vl.kts"))
       reportOutput.set(layout.buildDirectory.dir("vulnlog-reports"))
    }
    ```
4. Check if everything is set up correctly:
    ```terminaloutput
    ./gradlew :app:showCliVersion
    ```
   The output should be:
    ```terminaloutput
    > Task :app:showCliVersion
    Vulnlog $version
    
    BUILD SUCCESSFUL in 1s
    ```

Well done. Now you are ready to define your first vulnerability entry in Vulnlog.

## Define the first CVE vulnerability

Assume your OWASP Dependency-Check scanner found
the [Log4J vulnerability (CVE-2021-44228)](https://nvd.nist.gov/vuln/detail/cve-2021-44228) in your
project. Add the
finding to Vulnlog as follows.

1. Add the finding to the `app/app.vl.kts` file:
    ```kotlin
    val myFirstReleaseBranch by ReleaseBranchProvider
    
    val owaspDependencyCheck by ReporterProvider
    
    vuln("CVE-2021-44228") {
        report from owaspDependencyCheck at "2021-12-12" on myFirstReleaseBranch..myFirstReleaseBranch
    }
    ```
2. Generate your first report:
    ```terminaloutput
    ./gradlew generateReport
    ```
   You find the generated report in `app/build/vulnlog-reports/`:
    ```terminaloutput
    ls app/build/vulnlog-reports/
    report-myFirstReleaseBranch.html
    ```

When you open the report, the status is _under investigation_. This is because the report does not
have an `analyse` statement.

## Add CVE analysis and next actions

Let's assume the impact of the reported vulnerability is quite critical to your software project.
But no worries, your team already updated the dependency, and you are ready to ship the updated
release.

1. Update `app/app.vl.kts` accordingly:
    ```kotlin
    vuln("CVE-2021-44228") {
        report from owaspDependencyCheck at "2021-12-12" on myFirstReleaseBranch..myFirstReleaseBranch
        analysis verdict critical because """
            |Project uses Log4j extensively in authentication logging, exposing LDAP endpoints.
            |Remote attackers could exploit JNDI lookups for RCE, compromising entire system.""".trimMargin()
        task update "log4j-core" atLeastTo "2.16.0" on myFirstReleaseBranch
        execution fixedAt "2022-01-01" on myFirstReleaseBranch
    }
    ```

Now the status is _fixed_ and the next release, in this case version 0.1.0, is ready to go.

## Next steps

This was a relatively simple example. The DSL allows more complex handling of reported
vulnerabilities. Check out the [DSL Documentation](/documentation/dsl/) for examples and to get a
better insight of the capabilities of the Vulnlog DSL.
