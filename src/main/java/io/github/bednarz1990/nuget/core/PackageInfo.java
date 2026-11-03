package io.github.bednarz1990.nuget.core;

public record PackageInfo(String id, String version, String description) {
    @Override public String toString() { return id + "  " + version; }
}
