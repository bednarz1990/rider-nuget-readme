package io.github.bednarz1990.nuget;

import io.github.bednarz1990.nuget.core.*;
import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;

public final class CodeStylingChecks {
    public static void main(String[] args) throws Exception {
        String code = "// example\nvar name = \"<script>alert(1)</script>\";\nint count = 34;\n";
        String html = CodeStyling.highlight(SafeMarkdown.render("```csharp\r\n" + code + "```\r\n\r\nAfter code."));
        check(html.contains("syntax-keyword\">var</span>"), "C# keyword");
        check(html.contains("syntax-string"), "String literal");
        check(html.contains("syntax-number\">34</span>"), "Number literal");
        check(html.contains("syntax-comment\">// example</span>"), "Comment");
        check(!html.contains("<script>"), "Code remains escaped");
        check(html.contains("</pre><p>After code.</p>"), "Prose remains outside code");
        String plain = SafeMarkdown.render("```text\nvar name = 34;\n```\n\n`var`");
        check(CodeStyling.highlight(plain).equals(plain), "Unknown languages and inline code unchanged");
        String unsafe = CodeStyling.highlight(SafeMarkdown.render("<code class=\"language-csharp\" onclick=\"evil()\">x</code><script>evil()</script>"));
        check(!unsafe.contains("onclick") && !unsafe.contains("<script"), "Sanitizer remains enforced");
        SwingUtilities.invokeAndWait(() -> {
            try {
                JEditorPane pane = new JEditorPane();
                pane.setEditorKit(new HTMLEditorKit());
                pane.setText("<html><body>" + html + "</body></html>");
                String rendered = pane.getDocument().getText(0, pane.getDocument().getLength());
                check(rendered.contains(code.stripTrailing()), "Swing preserves code text and newlines");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        System.out.println("Code styling: 9 checks passed");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
