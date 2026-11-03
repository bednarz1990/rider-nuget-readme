package io.github.bednarz1990.nuget.core;

public record Readme(String path, String markdown) {
    public static Readme missing() { return new Readme("", ""); }
    public boolean available() { return !path.isEmpty(); }
}
