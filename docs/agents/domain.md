# Domain Docs

This repo uses a single-context domain documentation layout.

## Before exploring, read these

- **`GLOSSARY.md`** at the repo root, or **`GLOSSARY-MAP.md`** if it exists; read the glossary relevant to the topic.
- **`docs/adr/`**: read ADRs that touch the area you're about to work in.

If these files do not exist, proceed silently. Do not flag their absence or suggest creating them upfront. The domain-modeling skill creates them when terms or decisions are resolved.

## File structure

```text
/
├── GLOSSARY.md
└── docs/adr/
    ├── 0001-example-decision.md
    └── 0002-another-decision.md
```

## Use the glossary's vocabulary

When naming a domain concept in an issue title, refactor proposal, hypothesis, or test name, use the term defined in `GLOSSARY.md`. If the concept is missing, note the gap for domain modeling.

## Flag ADR conflicts

If proposed work contradicts an existing ADR, surface the conflict explicitly rather than silently overriding the decision.
