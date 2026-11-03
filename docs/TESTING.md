# Testing

## Automated

With JDK 21 and Gradle 9.1.0:

```sh
gradle test buildPlugin verifyPluginProjectConfiguration verifyPluginStructure
gradle verifyPlugin
gradle integrationTest
```

`CoreChecksTest` invokes the dependency-light `CoreChecks` suite. The same checks
can run against a locally installed SDK using `scripts/offline-check.ps1`.
Fixtures cover metadata/query parsing, versions, exact README paths, missing
declarations, corrupt/large archives, XXE/traversal, unsafe HTML/links, cache
expiration/eviction, failure retries, cancellation and stale-result races.
These fixture tests do not establish live NuGet availability or IDE integration.

`IntegrationProbe` searches Serilog, Newtonsoft.Json and Dapper, enumerates versions,
and loads each latest stable README or verifies the no-declaration state. It uses
real HTTPS and must not silently skip failures. CI runs it only on manual dispatch,
so external outages do not make every pull request nondeterministic.

## Manual acceptance (release gate)

1. Run `gradle runIde` on the target Rider, with a disposable test project.
2. Open View → Tool Windows → NuGet README. Check the empty state and keyboard focus.
3. Search Serilog; select a package/version; confirm name/version and content match NuGet.org.
4. Select an older version and confirm the text changes when its README differs.
5. Enable prereleases; confirm prerelease versions become available and stable filtering works.
6. Search a random nonexistent name: clear results and show the no-results message.
7. Open a version whose manifest has no README: show missing state, no stale contents.
8. Switch packages rapidly on a slow connection: old results must never replace current selection.
9. Disconnect the network: error/retry appears; reconnect and Retry succeeds.
10. Repeat a README selection and inspect network activity for a cache hit; Clear cache triggers a fetch.
11. Toggle light/dark themes. Check heading, code, table and link readability.
12. Select/copy code, inspect raw Markdown, and use Copy Markdown.
13. Click an HTTPS link: default browser opens. Images and embedded scripts must not run.
14. Close project while loading; verify no callbacks update disposed components and no executor leaks.
15. Confirm project files, package references and NuGet configuration remain unchanged.
16. Install the built ZIP through Settings → Plugins → Install Plugin from Disk, restart,
    and repeat steps 2–4. Capture an actual screenshot only after this succeeds.

Record Rider build, OS, plugin version, result and any IDE log errors. Do not broaden
the compatibility range or tag a stable release until these gates have passed.
