---
name: code-change-docs
description: Implement code changes and proactively add or update detailed Markdown documentation after completing the work. Use when the user values implementation plus docs, wants knowledge captured in docs/, or wants Markdown output to follow Obsidian-style conventions such as frontmatter, clear heading hierarchy, callouts, code fences, and mermaid diagrams when helpful.
---

# Code Change Docs Skill

Use this skill when the user wants code changes plus durable documentation, especially when they have shown a preference for explanations being captured in repository Markdown files.

## Primary Behavior

After completing a meaningful code change, do not stop at implementation alone. Also add or update documentation that helps future readers understand:

- what changed
- why it changed
- how the runtime behavior now works
- what validation was done
- what important edge cases or limitations remain

Prefer placing new documentation under `docs/` unless the repository already has a clearly better location.

## When To Write Or Update Docs

Add or update docs when any of the following is true:

- the task introduces a new workflow, endpoint, cache path, concurrency rule, or integration
- the implementation has non-obvious reasoning or tradeoffs
- the user asked conceptual follow-up questions during the task
- the project is being used for learning, onboarding, or interview preparation
- future maintainers would likely ask "why was it done this way?"

Docs can be skipped only when the change is trivial and self-evident, or when the user explicitly says not to create documentation.

## Documentation Scope

Choose the smallest doc set that still preserves the important knowledge:

- update an existing doc when the topic already has a natural home
- create a focused new doc when the concept deserves its own explanation
- keep docs task-oriented rather than turning them into changelogs

Useful topics include:

- request flow or sequence
- cache behavior
- Redis/MySQL responsibility split
- invalidation strategy
- concurrency guarantees
- example requests and responses
- testing and verification notes

## Obsidian-Style Markdown Conventions

When generating Markdown, follow the conventions summarized in [references/markdown-style.md](references/markdown-style.md).

Default expectations:

1. Start with YAML frontmatter when creating a new standalone doc.
2. Use a clean heading hierarchy with concise section names.
3. Use standard Markdown links for URLs and repository file paths unless the target is explicitly an Obsidian vault.
4. Use callouts for cautions, key takeaways, and implementation notes when they improve scanning.
5. Use fenced code blocks with language tags.
6. Use Mermaid diagrams for flows, sequences, or state transitions when they materially improve understanding.
7. Prefer short sections and scannable prose over dense walls of text.

## Suggested Doc Template

For a new implementation-focused doc, adapt this shape:

```markdown
---
title: Topic Name
tags:
  - project
  - implementation
  - backend
---

# Topic Name

## What This Covers

## Background

## Implementation

## Request Flow

## Important Behaviors

## Validation

## Limits Or Follow-ups
```

## Final Check Before You Finish

Before closing the task:

1. Confirm the implementation and docs are consistent with each other.
2. Make sure the doc explains intent, not just mechanics.
3. Verify examples, paths, endpoint names, and key names match the code.
4. Mention tests run, or explicitly note if they were not run.
