# Architecture

`ReadmeToolWindowFactory` creates content lazily and assigns `ReadmePanel` as its
disposer. The panel owns a bounded two-worker IDE executor, a `NuGetClient`, and a
generation-based `LatestRequest`. Search, version discovery, archive parsing and
Markdown generation run on workers. Swing mutations happen on the EDT.

Starting a request cancels the previous Future. A monotonically increasing ticket
also guards queued UI callbacks, so a completed old request cannot replace newer
content. Disposing the window invalidates tickets, shuts down work and removes
the look-and-feel listener. Package/version changes clear old content immediately.

`NuGetClient` separates service discovery and metadata from `Transport`. The MVP
transport restricts requests to NuGet.org HTTPS, prohibits redirects and attaches
no credentials/cookies. For future sources, replace this policy behind an explicit
source configuration and partition caches by source identity; never reuse a
NuGet.org cache key across authenticated sources or forward credentials to arbitrary hosts.

`PackageReadme` disables XML DTD/external entities and validates the manifest path.
It uses random-access `ZipFile`, only inflates the selected README, limits entries,
checks cancellation and deletes its temporary archive in `finally`. Entry names
are never used as filesystem destinations. The package body is bounded to 40 MiB
in the HTTP subscriber, manifests to 1 MiB, JSON to 4 MiB and README to 512 KiB.

`MemoryCache` holds at most 24 successful/missing results for 30 minutes, using
LRU eviction. Exceptions are not cached. The cache lives with one tool window.
With two concurrent workers and byte-array copies, peak memory can be materially
larger than 40 MiB; limits intentionally reject large packages.

`SafeMarkdown` converts GFM to HTML and passes it through a tag/attribute allowlist.
Only safe browser links survive; no image fetch, local-file URL, JavaScript, form,
iframe, embedded object or untrusted CSS is emitted. Theme CSS is application-owned.
The Markdown source tab displays plain text and supports copying exact examples.

No project writes, NuGet installation, SDK subprocesses, telemetry or persisted
credentials are performed by the plugin.
