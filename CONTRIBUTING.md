# Contributing

Use JDK 21 and Gradle 9.1.0. Run `gradle build buildPlugin verifyPluginStructure verifyPlugin`
before proposing a release. Add deterministic fixtures for changes to retrieval,
security, caching or cancellation; live network checks are opt-in.

Keep UI mutations on the Swing EDT and all network/parsing work off it. Dispose
executors and callbacks with tool-window content. Never render unsanitized HTML,
follow arbitrary metadata URLs, or log secrets. Keep changes to native Rider
integration behind APIs verified against actual SDK artifacts.

Open a focused pull request describing the user-visible change and actual tests
run. Do not claim compatibility based only on a successful Java compilation.
