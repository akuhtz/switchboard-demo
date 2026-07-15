# Model Railway Switchboard - AI Agent Guidelines

Guidelines for AI agents working on this codebase.

## Project Info

A Java Swing-based switchboard component for controlling and visualising a model railway layout.

- Version: 1.0-SNAPSHOT
- Java: 21+
- Build: Maven 3.9.12+

## AI Agent Rules of Engagement

These rules apply to ALL AI agents working on this codebase.

### Attribution

- All AI-generated content (GitHub PR descriptions, review comments, JIRA comments) MUST clearly
  identify itself as AI-generated and mention the human operator.
  Example: "_Claude Code on behalf of [Human Name]_"
- **Never guess or hallucinate the operator's name.** Always determine it programmatically:
  - Use `gh api /user --jq '.login'` to get the authenticated GitHub username.
  - If for any reason the lookup fails, omit the name rather than guessing.
- AI coding agents MUST be configured to add co-authorship trailers to commits
  (e.g., `Co-authored-by`). For Claude Code, enable this via the
  [attribution settings](https://code.claude.com/docs/en/settings#attribution-settings).

### Testing

- Use AssertJ assertions (`assertThat(...)`) in all tests. Do NOT use JUnit Jupiter
  assertions (`assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`, etc.).
