# Feasibility and integration decision

Research date: 2026-11-03. Decision: **Option C, independent tool window**.

## Evidence and boundaries

The official [Rider plugin guide](https://plugins.jetbrains.com/docs/intellij/rider.html)
describes the IntelliJ frontend and ReSharper backend. This feature needs only the
frontend. A backend/plugin protocol model would add deployment complexity without
solving an established need.

The [Rider extension-point list](https://plugins.jetbrains.com/docs/intellij/rider-extension-point-list.html)
lists `com.intellij.rider.nuget.actions.riderNuGetQuickListActionContributor`.
This is evidence of a NuGet quick-list contribution point, **not** evidence of a
public selected package/version data key or details-panel tab API. The general
`com.intellij.rider.ui.toolwindow.contentProvider` is marked Internal.

| Option | Decision | Reason |
| --- | --- | --- |
| A: native NuGet README tab | Deferred | No supported details-tab/selection contract established in reviewed documentation. Do not invent one. |
| B: NuGet context action | Deferred | Quick-list extension exists; reliable selection payload and Discover context are not verified. |
| C: independent tool window | Implemented | Public `ToolWindowFactory` and `com.intellij.toolWindow` provide a documented lazy UI lifecycle. |

This is not a claim that A/B are impossible. Inspect the actual target SDK's
NuGet implementation and action data context before revisiting them. The local
SDK is Rider 2023.3.4 (product-info build 233.14475.66), despite its historical
directory name; it does not prove what Rider 2026 exposes.

`javap` against available SDK artifacts verified `ToolWindowFactory`,
`ContentFactory.getInstance`, `MarkdownParser`, `GFMFlavourDescriptor`,
`HtmlGenerator` and `DefaultTagRenderer` signatures. All source compiled against
those artifacts. Compatibility of parser-library signatures in 2026 must still
pass Plugin Verifier; no reflection into Rider internals is used.

## Retrieval

Discover search and flat-container endpoints through the NuGet V3 service index.
Search uses `semVerLevel=2.0.0`. Version lists come from the flat container; do not
sort versions lexicographically. For a chosen normalized version, fetch its
`.nuspec` first and inspect `metadata/readme`. An absent declaration is a normal
missing README state. Download `.nupkg` only when needed, then read the exact
declared path (with backslash normalization) without extracting archive paths.

The [registration API](https://learn.microsoft.com/en-us/nuget/api/registration-base-url-resource)
is useful for version-specific package metadata. It is not selected as the source
of raw Markdown: a `readmeUrl` must not be assumed to return Markdown rather than
rendered HTML. The `.nuspec` plus `.nupkg` route gives explicit original contents
and avoids following arbitrary external README URLs. No undocumented gallery
README endpoint is hardcoded.

## Rendering and build

Use JetBrains' GFM parser, then a restrictive HTML allowlist and native Swing
`JEditorPane`. Only HTTP(S) links are actionable, and only on explicit clicks.
All images, executable content and arbitrary styles are discarded. The parser
comes from the platform/bundled Markdown dependency; it is not a JCEF browser
and does not invoke the Markdown plugin's internal editor preview.

The intended build uses IntelliJ Platform Gradle Plugin **2.19.0**, Gradle
**9.1.0**, JDK **25**, Java 25 bytecode, and Rider **2026.2.3.1**. The
[Gradle plugin requirements](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
state Gradle 9.0.0 minimum. Java source is a documented deviation from the preferred
Kotlin stack: it enabled actual compilation/testing with the available JDK, while
no Kotlin compiler or Gradle distribution could be downloaded in the local sandbox.
There is no language-level obstacle to migrating to Kotlin later.

## Other primary references

- [Tool window registration and lifecycle](https://plugins.jetbrains.com/docs/intellij/tool-windows.html)
- [JetBrains plugin template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [ReSharper Rider SDK guide](https://www.jetbrains.com/help/resharper/sdk/Rider.html)
- [NuGet package content](https://learn.microsoft.com/en-us/nuget/api/package-base-address-resource)
- [NuGet search](https://learn.microsoft.com/en-us/nuget/api/search-query-service-resource)
- [Nuspec README field](https://learn.microsoft.com/en-us/nuget/reference/nuspec#readme)
- [JetBrains Markdown parser](https://github.com/JetBrains/markdown)
- [Plugin verification configuration](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html#verify-plugin-ides)
- [Marketplace submission](https://plugins.jetbrains.com/docs/marketplace/uploading-a-new-plugin.html)

See VALIDATION.md for observed results, not inferred compatibility.
