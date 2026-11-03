package io.github.bednarz1990.nuget;

import io.github.bednarz1990.nuget.core.SafeMarkdown;

/** Regression for Windows NuGet READMEs with multiple fenced code blocks. */
public final class MarkdownChecks {
    public static void main(String[] args) {
        String markdown = "# Serilog\n\n```csharp\nvar log = new LoggerConfiguration();\n"
                + "Log.Information(\"Hello\");\n```\n\n"
                + "Text with a [link](https://serilog.net) and **bold**.\n\n"
                + "```csharp\nLog.CloseAndFlush();\n```\n\n"
                + "## After code\n\n- First\n- Second\n\n"
                + "| Name | Value |\n| --- | --- |\n| Test | 42 |\n";
        String expected = SafeMarkdown.render(markdown);
        for (String newline : new String[]{"\n", "\r\n", "\r"}) {
            String html = SafeMarkdown.render(markdown.replace("\n", newline));
            check(html.equals(expected), "Line endings must not change rendered HTML");
            check(!html.contains("```"), "Code fences must not leak into the preview");
            check(html.split("<pre>", -1).length == 3, "Two separate code blocks");
            check(html.contains("</pre><p>Text"), "Prose must be outside the code block");
            check(html.contains("<a href=\"https://serilog.net\">link</a>"), "Clickable link after code");
            check(html.contains("<h2>After code</h2>"), "Heading after closing fence");
            check(html.contains("<li>First</li>") && html.contains("<table>"), "Lists and tables after code");
            check(html.contains("LoggerConfiguration();\nLog.Information"), "Preserve newlines within code");
        }
        String unsafe = SafeMarkdown.render("```html\r\n<script>alert(1)</script>\r\n```\r\n\r\n"
                + "[bad](javascript:alert)\r\n<script>alert(2)</script>\r\n");
        check(!unsafe.contains("<script") && !unsafe.contains("href=\"javascript:"), "HTML remains sanitized");
        check(unsafe.contains("&lt;script&gt;"), "Code examples remain escaped");
        System.out.println("Markdown regression: 26 checks passed");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
