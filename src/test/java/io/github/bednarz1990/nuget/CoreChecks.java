package io.github.bednarz1990.nuget;

import io.github.bednarz1990.nuget.core.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;

/** Dependency-light suite, also invoked by JUnit in CI. No internet required. */
public final class CoreChecks {
    private static int passed;
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        passed++; System.out.println("PASS " + label);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void fails(Class<? extends Throwable> expected, Checked action, String label) throws Exception {
        try { action.run(); } catch (Throwable t) {
            if (expected.isInstance(t)) { check(true, label); return; }
            throw new AssertionError(label, t);
        }
        throw new AssertionError("Expected failure: " + label);
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static byte[] archive(String path, String content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("unrelated.txt")); zip.write(bytes("not a readme")); zip.closeEntry();
            zip.putNextEntry(new ZipEntry(path)); zip.write(bytes(content)); zip.closeEntry();
        }
        return output.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        passed = 0;
        manifests(); extraction(); rendering(); cache(); client(); cancellation(); endpoints();
        System.out.println("TOTAL: " + passed + " checks passed");
    }

    private static void manifests() throws Exception {
        check(PackageReadme.declaredPath(bytes("<package xmlns='http://schemas.microsoft.com/packaging/2013/05/nuspec.xsd'><metadata><readme>docs/Guide.md</readme></metadata></package>")).equals("docs/Guide.md"), "namespaced manifest and custom readme filename");
        check(PackageReadme.declaredPath(bytes("<package><metadata><readme>docs\\README.md</readme></metadata></package>")).equals("docs/README.md"), "Windows path normalization");
        check(PackageReadme.declaredPath(bytes("<package><metadata/></package>")).isEmpty(), "missing readme declaration");
        check(PackageReadme.declaredPath(bytes("<package><files><readme>wrong.md</readme></files><metadata/></package>")).isEmpty(), "only metadata readme is used");
        fails(IOException.class, () -> PackageReadme.declaredPath(bytes("<package><metadata><readme>../secret</readme></metadata></package>")), "path traversal rejected");
        fails(IOException.class, () -> PackageReadme.declaredPath(bytes("<package><metadata><readme>C:\\secret</readme></metadata></package>")), "absolute Windows path rejected");
        fails(IOException.class, () -> PackageReadme.declaredPath(bytes("<!DOCTYPE package [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><package><metadata><readme>&xxe;</readme></metadata></package>")), "XXE and DOCTYPE rejected");
    }

    private static void extraction() throws Exception {
        Readme value = PackageReadme.extract(archive("docs/Guide.md", "\uFEFF# Hello Łukasz"), "docs/Guide.md");
        check(value.markdown().equals("# Hello Łukasz") && value.available(), "ZIP custom path UTF8 and BOM");
        check(!PackageReadme.extract(new byte[0], "").available(), "no declared README avoids ZIP parsing");
        fails(IOException.class, () -> PackageReadme.extract(archive("other.md", "x"), "README.md"), "broken declaration is an error");
        fails(IOException.class, () -> PackageReadme.extract(archive("readme.md", "x"), "README.md"), "archive path case is preserved");
        fails(IOException.class, () -> PackageReadme.extract(archive("readme.md", "x".repeat(PackageReadme.README_LIMIT + 1)), "readme.md"), "oversized expanded README rejected");
        fails(IOException.class, () -> PackageReadme.extract(bytes("invalid zip"), "readme.md"), "corrupt ZIP rejected");
        Thread.currentThread().interrupt();
        try { fails(InterruptedException.class, () -> PackageReadme.extract(archive("readme.md", "x"), "readme.md"), "archive traversal cancellation"); }
        finally { Thread.interrupted(); }
    }

    private static void rendering() {
        String html = SafeMarkdown.render("# Heading\n\n- first\n- second\n\n```csharp\nConsole.WriteLine(\"<safe>\");\n```\n\n| A | B |\n|---|---|\n| 1 | 2 |\n\n[link](https://example.com)");
        check(html.contains("<h1>") && html.contains("<li>"), "Markdown headings and lists");
        check(html.contains("<pre>") && html.contains("&lt;safe&gt;"), "Markdown fenced code escaped");
        check(html.contains("<table>") && html.contains("<td>"), "GFM tables");
        check(html.contains("href=\"https://example.com\""), "safe Markdown links retained");
        String unsafe = SafeMarkdown.sanitize("<script>alert(1)</script><style>body{background:url(file:///secret)}</style><img src='https://evil.test/track'><iframe src='file:///secret'></iframe><a href='javascript:alert(1)' onclick='x()'>x</a><a href='file:///secret'>local</a><p style='background:url(https://evil.test)'>okay</p>");
        check(!unsafe.contains("<script") && !unsafe.contains("alert(1)") && !unsafe.contains("<style") && !unsafe.contains("<img") && !unsafe.contains("<iframe"), "active and remote resources omitted");
        check(!unsafe.contains("href=") && !unsafe.contains("onclick") && !unsafe.contains("style="), "unsafe links and attributes omitted");
        check(!SafeMarkdown.safeLink("file:///secret") && !SafeMarkdown.safeLink("javascript:alert(1)") && !SafeMarkdown.safeLink("data:text/html,x"), "unsafe URI schemes rejected");
        check(!SafeMarkdown.safeLink("https://user:password@example.com/") && !SafeMarkdown.safeLink("//example.com"), "credentialed and relative URLs rejected");
        check(SafeMarkdown.safeLink("https://example.com/docs?q=abc#section"), "HTTPS browser link accepted");
    }

    private static final class MutableClock extends Clock {
        long now;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now); }
    }
    private static void cache() {
        MutableClock clock = new MutableClock();
        MemoryCache<String, String> cache = new MemoryCache<>(2, Duration.ofSeconds(1), clock);
        cache.put("a", "A"); cache.put("b", "B"); cache.get("a"); cache.put("c", "C");
        check(cache.get("b") == null && cache.get("a").equals("A"), "cache evicts least recently used");
        clock.now = 1000;
        check(cache.get("a") == null, "cache expires at TTL boundary");
        cache.put("d", "D"); cache.clear(); check(cache.get("d") == null, "clear cache");
    }

    private static final class Fake implements Transport {
        final Map<String, byte[]> responses = new HashMap<>();
        final List<String> calls = new ArrayList<>();
        Fake() {
            responses.put("https://api.nuget.org/v3/index.json", bytes("{\"resources\":[{\"@type\":\"SearchQueryService/3.5.0\",\"@id\":\"https://search.nuget.org/query\"},{\"@type\":\"PackageBaseAddress/3.0.0\",\"@id\":\"https://api.nuget.org/v3-flatcontainer/\"}]}"));
        }
        public byte[] get(URI uri, int limit) throws IOException {
            calls.add(uri.toString());
            byte[] value = responses.get(uri.toString());
            if (value == null) throw new IOException("HTTP 404 fixture");
            if (value.length > limit) throw new IOException("fixture too large");
            return value;
        }
    }

    private static void client() throws Exception {
        Fake fake = new Fake(); NuGetClient client = new NuGetClient(fake);
        fake.responses.put("https://search.nuget.org/query?q=a+b&take=50&skip=0&semVerLevel=2.0.0&prerelease=false", bytes("{\"data\":[{\"id\":\"Sample\",\"version\":\"1.10.0\",\"description\":\"Demo\"}]}"));
        check(client.search("a b", false).get(0).description().equals("Demo"), "search metadata and URL encoding");
        String base = "https://api.nuget.org/v3-flatcontainer/sample/";
        fake.responses.put(base + "index.json", bytes("{\"versions\":[\"1.9.0\",\"1.10.0\",\"2.0.0-beta.2\",\"2.0.0-beta.10\"]}"));
        check(client.versions("Sample", false).equals(List.of("1.10.0", "1.9.0")), "stable version filtering and numeric API order");
        check(client.versions("Sample", true).get(0).equals("2.0.0-beta.10"), "prerelease version order retained");
        fake.responses.put(base + "1.10.0/sample.nuspec", bytes("<package><metadata><readme>docs/Intro.md</readme></metadata></package>"));
        fake.responses.put(base + "1.10.0/sample.1.10.0.nupkg", archive("docs/Intro.md", "# Package"));
        check(client.readme("Sample", "1.10.0").markdown().equals("# Package"), "end-to-end fixture README retrieval");
        int previous = fake.calls.size(); client.readme("SAMPLE", "1.10.0");
        check(fake.calls.size() == previous, "README cached with case-normalized package ID");
        fake.responses.put(base + "1.9.0/sample.nuspec", bytes("<package><metadata/></package>"));
        check(!client.readme("Sample", "1.9.0").available(), "missing README result");
        check(fake.calls.stream().noneMatch(x -> x.endsWith("sample.1.9.0.nupkg")), "no download when README undeclared");
        fails(IOException.class, () -> client.readme("Sample", "9.0.0"), "HTTP failure propagated");
        fake.responses.put(base + "9.0.0/sample.nuspec", bytes("<package><metadata/></package>"));
        check(!client.readme("Sample", "9.0.0").available(), "HTTP failures are not cached; retry succeeds");
        fake.responses.put(base + "index.json", bytes("not json"));
        fails(IOException.class, () -> client.versions("Sample", true), "malformed JSON handled");
        fails(IllegalArgumentException.class, () -> client.versions("../../private", true), "path injection in ID rejected");
    }

    private static void cancellation() throws Exception {
        LatestRequest request = new LatestRequest();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1), interrupted = new CountDownLatch(1);
        try {
            long old = request.next();
            Future<?> future = executor.submit(() -> {
                started.countDown();
                try { new CountDownLatch(1).await(); } catch (InterruptedException e) { interrupted.countDown(); Thread.currentThread().interrupt(); }
            });
            request.attach(old, future); check(started.await(2, TimeUnit.SECONDS), "background request started");
            long current = request.next();
            check(interrupted.await(2, TimeUnit.SECONDS) && future.isCancelled(), "new request interrupts old work");
            check(!request.current(old) && request.current(current), "stale queued UI result rejected");
            FutureTask<Void> late = new FutureTask<>(() -> null);
            request.attach(old, late); check(late.isCancelled(), "late attachment race cancelled");
            request.next(); check(!request.current(current), "disposal invalidates queued result");
        } finally { executor.shutdownNow(); }
    }

    private static void endpoints() throws Exception {
        check(NuGetHttp.trusted(URI.create("https://api.nuget.org/v3/index.json")) != null, "NuGet HTTPS allowed");
        for (String url : List.of("http://api.nuget.org/", "https://nuget.org.evil.test/", "https://evilnuget.org/", "https://localhost/", "https://api.nuget.org:8443/", "https://secret@api.nuget.org/", "file:///tmp/secret")) {
            fails(IllegalArgumentException.class, () -> NuGetHttp.trusted(URI.create(url)), "endpoint rejected: " + url);
        }
    }
}
