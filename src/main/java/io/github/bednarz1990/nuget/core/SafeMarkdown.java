package io.github.bednarz1990.nuget.core;

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor;
import org.intellij.markdown.html.HtmlGenerator;
import org.intellij.markdown.parser.MarkdownParser;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.*;
import java.net.URI;
import java.util.Set;

/** Markdown parser followed by an HTML allowlist. Never load HTML directly into Swing. */
public final class SafeMarkdown {
    private static final Set<String> TAGS = Set.of("p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li",
            "pre", "code", "blockquote", "strong", "b", "em", "i", "s", "del", "table", "tr", "td", "th", "br", "hr", "a");
    public static String render(String markdown) {
        GFMFlavourDescriptor flavour = new GFMFlavourDescriptor();
        String html = new HtmlGenerator(markdown, new MarkdownParser(flavour).buildMarkdownTreeFromString(markdown), flavour, false)
                .generateHtml(new HtmlGenerator.DefaultTagRenderer((node, text, attributes) -> attributes, false));
        return sanitize(html);
    }
    public static boolean safeLink(String value) {
        try {
            URI uri = URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException e) { return false; }
    }
    public static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    public static String sanitize(String html) {
        StringBuilder result = new StringBuilder();
        try {
            new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
                private int hidden;
                private boolean dangerous(HTML.Tag tag) { return tag == HTML.Tag.SCRIPT || tag == HTML.Tag.STYLE; }
                public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
                    if (dangerous(tag)) { hidden++; return; }
                    if (hidden != 0 || !TAGS.contains(tag.toString())) return;
                    result.append('<').append(tag);
                    Object href = attrs.getAttribute(HTML.Attribute.HREF);
                    if (tag == HTML.Tag.A && href != null && safeLink(href.toString())) result.append(" href=\"").append(escape(href.toString())).append('"');
                    result.append('>');
                }
                public void handleEndTag(HTML.Tag tag, int pos) {
                    if (dangerous(tag)) { hidden = Math.max(0, hidden - 1); return; }
                    if (hidden == 0 && TAGS.contains(tag.toString())) result.append("</").append(tag).append('>');
                }
                public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
                    if (hidden == 0 && (tag == HTML.Tag.BR || tag == HTML.Tag.HR)) result.append('<').append(tag).append('>');
                    // Images, forms, frames, objects, SVG and all attributes except safe links are omitted.
                }
                public void handleText(char[] text, int pos) { if (hidden == 0) result.append(escape(new String(text))); }
            }, true);
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return result.toString();
    }
}
