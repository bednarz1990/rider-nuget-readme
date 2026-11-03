# GitHub and Marketplace

Intended public repository: https://github.com/bednarz1990/rider-nuget-readme

## Publish source

If importing this source bundle yourself, create an empty public repository named
`rider-nuget-readme` in your account, then use:

```sh
git init -b main
git add .
git commit -m "Add NuGet README Preview development preview"
git remote add origin https://github.com/bednarz1990/rider-nuget-readme.git
git push -u origin main
```

Do not run initialization commands over an existing checkout. Build outputs and
credentials are ignored. Review the Actions results; fix failures before tagging.

## Release

Complete docs/TESTING.md on the exact supported Rider build. Update version in
build.gradle.kts and plugin.xml, change notes and changelog. Tag `v0.1.0` only when
the version matches. The release workflow builds, tests and verifies, then creates
a **draft** GitHub release with the plugin ZIP. Review and publish the draft manually.
No workflow uploads to JetBrains Marketplace.

## Marketplace submission

1. Review vendor name, source URL, stable ID `io.github.bednarz1990.nugetreadme`,
   description, icon and MIT license. Confirm that the ID is available.
2. Run target-platform build, structure validation and Plugin Verifier; perform
   the manual acceptance checklist. Include only versions actually verified.
3. Prepare real screenshots and a concise support/privacy description. The
   description in META-INF/plugin.xml is the initial Marketplace copy.
4. Sign the release if required using `gradle signPlugin`, with `CERTIFICATE_CHAIN`,
   `PRIVATE_KEY`, and `PRIVATE_KEY_PASSWORD` supplied through your secret store.
5. In your Marketplace vendor account, use Upload new plugin and select the tested
   ZIP. Choose free distribution, MIT, source repository and support URL.
6. Review Marketplace validation/moderation and resolve issues before publication.
7. For later uploads, `gradle publishPlugin` requires a `PUBLISH_TOKEN`. Run it only
   with the maintainer's explicit authorization. Never commit tokens or signing keys.

References: [upload a new plugin](https://plugins.jetbrains.com/docs/marketplace/uploading-a-new-plugin.html),
[plugin signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html),
[upload API](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
