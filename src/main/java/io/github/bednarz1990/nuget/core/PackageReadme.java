package io.github.bednarz1990.nuget.core;

import org.w3c.dom.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Reads ZIP entries in memory; never extracts archive paths onto the filesystem. */
public final class PackageReadme {
    public static final int README_LIMIT = 512 * 1024;
    public static final int PACKAGE_LIMIT = 40 * 1024 * 1024;

    public static String declaredPath(byte[] nuspec) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(nuspec));
            Element root = document.getDocumentElement();
            for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (!(n instanceof Element) || !"metadata".equals(n.getLocalName())) continue;
                for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child instanceof Element && "readme".equals(child.getLocalName())) return safePath(child.getTextContent().trim());
                }
            }
            return "";
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("Invalid or unsafe package manifest.", e); }
    }

    static String safePath(String path) throws IOException {
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":") || normalized.indexOf('\0') >= 0
                || Arrays.asList(normalized.split("/", -1)).contains(".."))
            throw new IOException("Unsafe README path in package manifest.");
        return normalized;
    }

    public static Readme extract(byte[] archive, String path) throws IOException, InterruptedException {
        if (path.isEmpty()) return Readme.missing();
        if (archive.length > PACKAGE_LIMIT) throw new IOException("Package is larger than 40 MiB.");
        String expected = safePath(path);
        // ZipFile random access avoids inflating huge unrelated entries (zip bombs).
        Path temporary = Files.createTempFile("nuget-readme-", ".nupkg");
        try {
            Files.write(temporary, archive);
            try (ZipFile zip = new ZipFile(temporary.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                ZipEntry found = null;
                int count = 0;
                while (entries.hasMoreElements()) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    if (++count > 100_000) throw new IOException("Package contains too many entries.");
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().replace('\\', '/').equals(expected)) {
                        if (found != null) throw new IOException("Package has ambiguous README entries.");
                        found = entry;
                    }
                }
                if (found == null) throw new IOException("The README declared by this package is missing from its archive.");
                if (found.getSize() > README_LIMIT) throw new IOException("README is larger than 512 KiB.");
                try (InputStream input = zip.getInputStream(found)) {
                    byte[] data = input.readNBytes(README_LIMIT + 1);
                    if (data.length > README_LIMIT) throw new IOException("README is larger than 512 KiB.");
                    String text = new String(data, StandardCharsets.UTF_8);
                    if (text.startsWith("\uFEFF")) text = text.substring(1);
                    return new Readme(expected, text);
                }
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
