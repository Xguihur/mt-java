---
title: Obsidian-Style Markdown Conventions
tags:
  - writing
  - markdown
  - obsidian
---

# Obsidian-Style Markdown Conventions

This reference captures the parts of the local `obsidian-markdown` skill that are most useful when writing repository documentation.

## Goals

- make docs easy to scan
- keep them structurally consistent
- preserve enough context for future readers
- use richer Markdown features only when they add clarity

## Frontmatter

For a new standalone document, add YAML frontmatter at the top.

Recommended properties:

```yaml
---
title: Redis As Database Protection Layer
tags:
  - redis
  - caching
  - backend
---
```

Use short, practical tags. Do not add decorative metadata just because frontmatter exists.

## Headings

- Use one `#` title heading that matches the document topic.
- Prefer short section headings.
- Keep heading depth shallow when possible.
- Split long topics into several sections rather than oversized paragraphs.

## Links

- Use standard Markdown links for external URLs.
- Use standard Markdown links for repository paths unless the doc is intended for an actual Obsidian vault.
- Use wikilinks only when the target environment is explicitly Obsidian and the linked notes exist in that vault.

## Callouts

Use callouts sparingly for information that benefits from visual emphasis.

Examples:

```markdown
> [!note]
> This cache key is rebuilt on the next read after invalidation.

> [!warning]
> Updating the database without deleting the cache can serve stale data.

> [!tip]
> A short Mermaid sequence diagram is often clearer than a long paragraph.
```

## Code Blocks

- Always use fenced code blocks.
- Include a language tag when possible.
- Prefer small examples that illustrate the exact behavior being discussed.

## Diagrams

Use Mermaid when one of these is true:

- request flow is easier to understand visually
- there are two or more competing execution paths
- sequence or invalidation timing matters

Good candidates:

- cache hit vs cache miss
- write-then-delete-cache flow
- Redis Lua sequence
- single JVM vs multi JVM comparisons

## Writing Style

- explain intent before details
- prefer concrete examples over abstract statements
- keep paragraphs short
- avoid filler and repeated summary
- make tradeoffs explicit when they exist

## Repository Docs Adaptation

When writing docs for a code repository rather than a pure Obsidian vault:

- keep Obsidian-style structure and cleanliness
- do not force wikilinks if they are not natural for the repo
- favor portability so the Markdown still reads well on GitHub and local editors

## Minimal Checklist

Before finalizing a Markdown doc:

1. Does the title clearly describe the topic?
2. Does the doc explain both what changed and why?
3. Are examples consistent with the implementation?
4. Is there at least one section covering behavior or flow?
5. Would a teammate understand the feature without rereading the code first?
