package io.github.bednarz1990.nuget.core;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Basic C# lexical highlighting, applied only to sanitized fenced code. */
public final class CodeStyling {
    private static final Pattern BLOCK = Pattern.compile("<pre><code class=\"language-csharp\">(.*?)</code></pre>", Pattern.DOTALL);
    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "as", "async", "await", "base", "bool", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "decimal", "default", "delegate", "do", "double",
            "else", "enum", "event", "false", "finally", "float", "for", "foreach", "get", "if",
            "in", "int", "interface", "internal", "is", "lock", "long", "namespace", "new", "null",
            "object", "out", "override", "params", "partial", "private", "protected", "public",
            "readonly", "record", "ref", "return", "sealed", "set", "short", "static", "string",
            "struct", "switch", "this", "throw", "true", "try", "typeof", "uint", "ulong", "using",
            "var", "virtual", "void", "while", "yield", "required", "init");

    public static String highlight(String sanitizedHtml) {
        Matcher blocks = BLOCK.matcher(sanitizedHtml);
        StringBuilder result = new StringBuilder();
        while (blocks.find()) {
            String code = blocks.group(1).replace("&lt;", "<").replace("&gt;", ">")
                    .replace("&quot;", "\"").replace("&amp;", "&");
            blocks.appendReplacement(result, Matcher.quoteReplacement("<pre><code>" + tokens(code) + "</code></pre>"));
        }
        blocks.appendTail(result);
        return result.toString();
    }

    private static String tokens(String code) {
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < code.length();) {
            int start = i;
            char c = code.charAt(i);
            String kind = null;
            if (code.startsWith("//", i)) {
                int end = code.indexOf('\n', i); i = end < 0 ? code.length() : end; kind = "comment";
            } else if (code.startsWith("/*", i)) {
                int end = code.indexOf("*/", i + 2); i = end < 0 ? code.length() : end + 2; kind = "comment";
            } else if (c == '"' || c == '\'' || code.startsWith("@\"", i)) {
                boolean verbatim = c == '@';
                char quote = verbatim ? '"' : c;
                i += verbatim ? 2 : 1;
                while (i < code.length()) {
                    char next = code.charAt(i++);
                    if (!verbatim && next == '\\' && i < code.length()) { i++; continue; }
                    if (next == quote) {
                        if (verbatim && i < code.length() && code.charAt(i) == '"') { i++; continue; }
                        break;
                    }
                }
                kind = "string";
            } else if (Character.isJavaIdentifierStart(c)) {
                i++;
                while (i < code.length() && Character.isJavaIdentifierPart(code.charAt(i))) i++;
                if (KEYWORDS.contains(code.substring(start, i))) kind = "keyword";
            } else if (Character.isDigit(c)) {
                i++;
                while (i < code.length() && (Character.isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '.' || code.charAt(i) == '_')) i++;
                kind = "number";
            } else { i++; }
            String text = SafeMarkdown.escape(code.substring(start, i));
            if (kind == null) html.append(text);
            else html.append("<span class=\"syntax-").append(kind).append("\">").append(text).append("</span>");
        }
        return html.toString();
    }
}
