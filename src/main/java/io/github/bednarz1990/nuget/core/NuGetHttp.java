package io.github.bednarz1990.nuget.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.io.ByteArrayOutputStream;

/** No credentials, cookies or redirects. Restricted to public NuGet.org HTTPS resources. */
public final class NuGetHttp implements Transport {
    private volatile HttpClient client;
    private synchronized HttpClient client() {
        if (client == null) client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
        return client;
    }

    public static URI trusted(URI uri) {
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                || !(host.equalsIgnoreCase("nuget.org") || host.toLowerCase(java.util.Locale.ROOT).endsWith(".nuget.org"))
                || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443))
            throw new IllegalArgumentException("Only public NuGet.org HTTPS endpoints are supported.");
        return uri;
    }

    @Override public byte[] get(URI uri, int max) throws IOException, InterruptedException {
        trusted(uri);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(45))
                .header("User-Agent", "NuGetReadmePreview/0.1.0").GET().build();
        HttpResponse<byte[]> response = client().send(request, info -> new LimitedBody(max));
        if (response.statusCode() != 200) throw new IOException("NuGet.org returned HTTP " + response.statusCode() + ". Try again later.");
        return response.body();
    }

    /** Bounds the body while downloading, without an unbounded ofByteArray allocation. */
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final int max;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;
        LimitedBody(int max) { this.max = max; }
        public CompletionStage<byte[]> getBody() { return result; }
        public void onSubscribe(Flow.Subscription s) { subscription = s; s.request(1); }
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer b : buffers) {
                if (b.remaining() > max - bytes.size()) {
                    subscription.cancel(); result.completeExceptionally(new IOException("Download exceeds the size limit.")); return;
                }
                byte[] chunk = new byte[b.remaining()]; b.get(chunk); bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        public void onError(Throwable t) { result.completeExceptionally(t); }
        public void onComplete() { result.complete(bytes.toByteArray()); }
    }
}
