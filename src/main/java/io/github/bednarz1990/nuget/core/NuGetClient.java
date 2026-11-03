package io.github.bednarz1990.nuget.core;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

public final class NuGetClient {
    private record Endpoints(String search, String packages) {}
    private final Transport transport;
    private volatile Endpoints endpoints;
    private final MemoryCache<String, Readme> cache = new MemoryCache<>(24, Duration.ofMinutes(30), Clock.systemUTC());
    public NuGetClient(Transport transport) { this.transport = transport; }

    private JsonObject json(String url) throws IOException, InterruptedException {
        try {
            return JsonParser.parseString(new String(transport.get(URI.create(url), 4 * 1024 * 1024), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) { throw new IOException("NuGet.org returned invalid metadata.", e); }
    }

    private Endpoints endpoints() throws IOException, InterruptedException {
        if (endpoints != null) return endpoints;
        String search = null, packages = null;
        JsonArray resources = json("https://api.nuget.org/v3/index.json").getAsJsonArray("resources");
        if (resources == null) throw new IOException("NuGet service index has no resources.");
        for (JsonElement resource : resources) {
            JsonObject object = resource.getAsJsonObject();
            String type = string(object, "@type");
            if (type.startsWith("SearchQueryService")) search = string(object, "@id");
            if (type.equals("PackageBaseAddress/3.0.0")) packages = string(object, "@id");
        }
        if (search == null || packages == null) throw new IOException("NuGet service endpoints are unavailable.");
        NuGetHttp.trusted(URI.create(search)); NuGetHttp.trusted(URI.create(packages));
        Endpoints result = new Endpoints(search, packages.endsWith("/") ? packages : packages + "/");
        endpoints = result;
        return result;
    }

    public List<PackageInfo> search(String query, boolean prerelease) throws IOException, InterruptedException {
        String endpoint = endpoints().search();
        JsonArray data = json(endpoint + (endpoint.contains("?") ? "&" : "?") + "q="
                + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8) + "&take=50&skip=0&semVerLevel=2.0.0&prerelease=" + prerelease).getAsJsonArray("data");
        if (data == null) throw new IOException("NuGet search response has no data.");
        List<PackageInfo> result = new ArrayList<>();
        for (JsonElement entry : data) {
            JsonObject object = entry.getAsJsonObject();
            String id = string(object, "id");
            if (!id.isEmpty()) result.add(new PackageInfo(id, string(object, "version"), string(object, "description")));
        }
        return List.copyOf(result);
    }

    public List<String> versions(String id, boolean prerelease) throws IOException, InterruptedException {
        JsonArray data = json(endpoints().packages() + segment(id) + "/index.json").getAsJsonArray("versions");
        if (data == null) throw new IOException("NuGet version response has no versions.");
        List<String> result = new ArrayList<>();
        for (JsonElement v : data) {
            String version = v.getAsString();
            if (prerelease || !version.contains("-")) result.add(version);
        }
        // Flat-container versions are returned in ascending version order by NuGet.org.
        Collections.reverse(result);
        return List.copyOf(result);
    }

    public Readme readme(String id, String version) throws IOException, InterruptedException {
        String key = segment(id) + "/" + segment(version);
        Readme cached = cache.get(key);
        if (cached != null) return cached;
        String base = endpoints().packages() + key + "/";
        byte[] manifest = transport.get(URI.create(base + segment(id) + ".nuspec"), 1024 * 1024);
        String path = PackageReadme.declaredPath(manifest);
        Readme result = path.isEmpty() ? Readme.missing() : PackageReadme.extract(
                transport.get(URI.create(base + segment(id) + "." + segment(version) + ".nupkg"), PackageReadme.PACKAGE_LIMIT), path);
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        cache.put(key, result);
        return result;
    }

    public void clearCache() { cache.clear(); }
    private static String segment(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._+-]{0,199}")) throw new IllegalArgumentException("Invalid package ID or version.");
        return value.toLowerCase(Locale.ROOT);
    }
    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }
}
