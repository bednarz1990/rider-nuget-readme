package io.github.bednarz1990.nuget.core;

import java.io.IOException;
import java.net.URI;

@FunctionalInterface
public interface Transport {
    byte[] get(URI uri, int maximumBytes) throws IOException, InterruptedException;
}
