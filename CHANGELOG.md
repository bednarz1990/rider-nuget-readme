# Changelog

## 0.1.2 — Code preview styling

- Add code block backgrounds, borders and padding for light and dark themes.
- Add basic C# highlighting for keywords, strings, numbers and comments in csharp/cs/c# fences.
- Keep unknown languages as plain code and preserve original Markdown.
- Verify rendered Swing text and HTML escaping with regression checks.

## 0.1.1 — Markdown line endings

- Normalize CRLF and CR line endings before parsing Markdown, fixing unclosed code blocks in Windows-authored READMEs such as Serilog.
- Preserve original Markdown for viewing and copying.
- Add regression checks for code fences, prose, links, headings, lists, tables and HTML filtering across line endings.

## 0.1.0 — Development preview

- Add independent NuGet README tool window with package/version selection.
- Retrieve declared README from public NuGet package archives.
- Render filtered Markdown with native theme colors and copy support.
- Add bounded downloads/cache, cancellation, fixtures and CI/release workflows.
- Rider 2026 runtime verification remains a release gate; see docs/VALIDATION.md.
