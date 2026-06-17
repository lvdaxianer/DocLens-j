## Context

The current project documentation explains technical usage: APIs, SDK usage,
configuration, development, packaging, and OpenWebUI contracts. It does not
yet preserve the product direction discussed for DocLens-j.

Without a roadmap document, future implementation may drift toward a complete
SaaS-style admin product, including personal login and RBAC. The intended
direction is narrower: DocLens-j should remain a document parsing
infrastructure component that can be safely called by multiple systems.

## Goals / Non-Goals

**Goals:**

- Record the three-stage product roadmap in a reviewable repository file.
- Make the Stage 2 identity model explicit: caller credentials and request
  attribution, not personal user login.
- Give each stage concrete priorities, user value, acceptance checks, and
  success metrics.
- Preserve MVP boundaries so future work stays focused.

**Non-Goals:**

- Do not create implementation tasks for every roadmap item.
- Do not modify code, APIs, database migrations, or Dashboard views.
- Do not introduce a login system, user table, role model, or billing model.

## Decisions

- Place the roadmap at `docs/product-roadmap.md` so it sits beside existing
  product-facing technical documentation.
- Use Chinese as the primary language because the product discussion and
  repository README entry point are Chinese.
- Describe Stage 2 as "接入方凭证与调用治理" and explicitly exclude personal
  login/RBAC from the current product direction.
- Keep the document actionable: tables should include priority, value, and
  acceptance evidence rather than only broad themes.

## Risks / Trade-offs

- A roadmap can become stale. The document should name measurable indicators
  and MVP boundaries so future changes can update it deliberately.
- Product planning is not an implementation spec. The document should avoid
  pretending that all roadmap items are already committed engineering tasks.
- Keeping DocLens-j as infrastructure means some account-level authorization
  needs remain in host systems. The roadmap should make that product boundary
  clear.
