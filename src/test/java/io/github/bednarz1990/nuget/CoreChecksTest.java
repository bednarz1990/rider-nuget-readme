package io.github.bednarz1990.nuget;

import org.junit.jupiter.api.Test;

class CoreChecksTest {
    @Test void coreBehaviorAndSecurity() throws Exception { CoreChecks.main(new String[0]); }
    @Test void markdownLineEndings() { MarkdownChecks.main(new String[0]); }
    @Test void codeStyling() throws Exception { CodeStylingChecks.main(new String[0]); }
}
