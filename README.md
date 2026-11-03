# NuGet README Preview

Read a NuGet.org package's documentation in JetBrains Rider before installing it.

The plugin adds **View → Tool Windows → NuGet README**. Search for a package,
select a version, and read its Markdown README in a native Swing preview.
Package installation remains in Rider's existing NuGet window.

**Status: development preview.** The first local build was compiled against
Rider 2023.3.4's SDK, with 50 offline behavioral/security checks passing.
The intended target is **Rider 2026.2**, build 262; target-platform compilation,
Plugin Verifier, live NuGet tests and interactive acceptance are separate gates.
Do not interpret the declared compatibility range as a verified support claim.
See [validation results](docs/VALIDATION.md) and the GitHub Actions run.

## Features

- Public NuGet.org search, up to 50 results, with optional prereleases.
- Version selection before installation, including versions from the flat container.
- Original README retrieval from the path declared in the package's `.nuspec`.
- Headings, lists, fenced code, links and GFM tables; light/dark theme colors.
- Select and copy rendered text, or use the Markdown tab and Copy Markdown button.
- Loading, empty, missing README, error and retry states.
- Project-local LRU cache, capped at 24 README entries with a 30-minute TTL.
- Cancellable background work and generation checks that reject stale results.

No package is installed. No project or NuGet configuration is modified. The only
temporary disk data is a downloaded `.nupkg`, removed after reading its README.
There is no telemetry, account sign-in, or credential handling.

## Install

After CI and the manual acceptance checklist pass, download the plugin ZIP from
the successful Actions run's `rider-nuget-readme-plugin` artifact (extract the
outer GitHub artifact ZIP first), or from a reviewed GitHub release.

1. In Rider, open **Settings → Plugins → gear menu → Install Plugin from Disk**.
2. Select `rider-nuget-readme-0.1.0.zip` and restart if requested.
3. Ensure Rider's bundled Markdown plugin is enabled.
4. Open a project, then **View → Tool Windows → NuGet README**.
5. Search for `Serilog`, select a result and version, and read its README.

This is not yet published on JetBrains Marketplace. An `offline-candidate` ZIP,
if supplied separately, is for inspection/testing only and is not a verified release.

## Build from source

Requirements: JDK 25, Gradle 9.1.0, network access to Gradle Plugin Portal,
Maven Central, JetBrains downloads and NuGet.org for optional integration tests.

```sh
gradle build buildPlugin verifyPluginProjectConfiguration verifyPluginStructure
gradle verifyPlugin
gradle integrationTest  # explicit live network test: Serilog, Newtonsoft.Json, Dapper
gradle runIde
```

Output: `build/distributions/rider-nuget-readme-0.1.0.zip`.
The project uses IntelliJ Platform Gradle Plugin 2.19.0 and Java source with
Gradle Kotlin DSL. Java was chosen to enable an actual offline compilation in
the available environment; there is no Kotlin compiler installed locally.
The runtime uses JetBrains' Markdown parser and the platform-provided Gson.

A Gradle wrapper binary is not fabricated or bundled. With trusted Gradle installed,
generate it using `gradle wrapper --gradle-version 9.1.0`, then commit the generated
wrapper files after validating their checksums. CI installs the pinned Gradle version.

## Limits and safety

- Separate tool window; no automatic synchronization with the native NuGet selection.
- Public NuGet.org only, HTTPS only, no automatic redirects, cookies or credentials.
- Package download limit 40 MiB; README limit 512 KiB. Large SDK packages may be refused.
- Missing `.nuspec` README declarations are shown as unavailable. Filenames are not guessed.
- Remote images, relative links, scripts, embedded objects, custom styles and unsafe URI schemes are disabled.
- Swing HTML rendering is intentionally simpler than a web browser: basic C# syntax highlighting only; no task-box interaction or per-code-block copy button.
- Flat-container versions can include unlisted versions; package search excludes them.
- Standard JDK proxy behavior is used; Rider-specific proxy/authenticated feed integration is not implemented.
- Remote-development/split-mode behavior is not verified.

## Project layout

```text
.github/workflows/    CI verification and tagged draft releases
docs/                Feasibility, architecture, testing, validation, publication
scripts/             Reproducible offline build/check helper
src/main/java/       NuGet client, retrieval, cache, renderer and native UI
src/main/resources/  Plugin descriptor and icons
src/test/java/       Behavioral/security fixtures and optional live probes
build.gradle.kts     Build and Marketplace configuration
```

Screenshot placeholder: add an actual Rider 2026.2 screenshot after completing
the manual checklist. No simulated screenshots or compatibility badges are used.

## Contributing and roadmap

See [CONTRIBUTING.md](CONTRIBUTING.md), [architecture](docs/ARCHITECTURE.md) and
[testing](docs/TESTING.md). Priorities are verified 2026 support, native NuGet context
integration when a supported selection API is available, package-search pagination,
and authenticated sources through the IDE credential store. Private-feed credentials
must never be passed to external README URLs.

Licensed under [MIT](LICENSE). Unofficial plugin, not affiliated with NuGet or JetBrains.
