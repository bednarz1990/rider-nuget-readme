# Validation record

Date: 2026-11-03.

| Check | Actual result |
| --- | --- |
| Compile all production Java | Passed using JBR javac 17.0.10 and local Rider SDK 233.14475.66 (2023.3.4) |
| Offline behavioral/security suite | 50 checks passed, exit code 0 |
| Live NuGet integration | Attempt failed before HTTP: local sandbox denied loopback socket required by JDK HttpClient |
| Local Gradle build | Not run: no Gradle distribution/cache and dependency downloads unavailable |
| Rider 2026 compilation | Pending GitHub Actions / connected build environment |
| Plugin structure validation task | Pending Gradle |
| Plugin Verifier for Rider 2026 | Pending Gradle |
| Interactive Rider test | Not run |
| Marketplace publication | Not performed |

The local SDK path has a historical `2022.2.4` directory name, but its product-info.json
reports **2023.3.4**. Results use the product metadata, not the directory name.

The initial Markdown test found a null customizer passed to JetBrains' renderer;
it was replaced by an identity customizer and the complete suite passed.

An offline candidate package, when produced by `scripts/offline-check.ps1`, contains
only project classes/resources. It uses the declared 262 target range, but was
compiled against 233, and therefore is **not** a verified Rider 2026 distribution.
Do not submit that candidate to Marketplace. The canonical distribution must come
from a successful target-SDK Gradle build and Plugin Verifier run.
