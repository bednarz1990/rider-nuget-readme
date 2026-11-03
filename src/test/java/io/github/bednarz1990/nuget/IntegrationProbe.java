package io.github.bednarz1990.nuget;

import io.github.bednarz1990.nuget.core.*;

/** Explicit opt-in live test. Failures must not be presented as passing checks. */
public final class IntegrationProbe {
    public static void main(String[] args) throws Exception {
        NuGetClient client = new NuGetClient(new NuGetHttp());
        for (String id : new String[]{"Serilog", "Newtonsoft.Json", "Dapper"}) {
            var results = client.search(id, false);
            if (results.stream().noneMatch(p -> p.id().equalsIgnoreCase(id))) throw new AssertionError("Search missing " + id);
            var versions = client.versions(id, false);
            if (versions.isEmpty()) throw new AssertionError("No versions for " + id);
            var readme = client.readme(id, versions.get(0));
            if (readme.available() && SafeMarkdown.render(readme.markdown()).isBlank()) throw new AssertionError("Empty rendered README for " + id);
            System.out.println(id + " " + versions.get(0) + ": " + (readme.available() ? readme.path() : "no declared README"));
        }
    }
}
