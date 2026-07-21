## 1. Public Beta Version Contract

- [x] 1.1 Add failing release-contract checks, synchronize `0.1.0-beta.1`
  across Maven, Dashboard, versioned architecture image tags, x86 Compose,
  Helm and packaging documentation, then pass focused and broader verification.

## Task Boundary And Agent Dispatch

- Module-oriented agent: `release-packaging`.
- Owned responsibility: the public beta version contract and local deployment
  defaults.
- Allowed files: Maven POM files, Dashboard package metadata, local image build
  and verification scripts, x86 Compose and README, Helm metadata and values,
  packaging documentation, and this OpenSpec change.
- Out of scope: business runtime code, database migrations, image rebuilds,
  registry publishing, and deployment to `10.100.30.144`.
- Dependencies: existing local runtime archives, Maven/npm toolchains, Docker
  contract scripts, and Helm CLI.
- Focused verification: `scripts/verify-local-runtime-dockerfile.sh`,
  `scripts/verify-x86-runtime-delivery-layout.sh`, version metadata queries, and
  `helm lint deploy/helm/doclens-j`.
- Broader verification: Maven tests, Dashboard tests and build, packaging docs
  checks, shell syntax checks, and `git diff --check`.
- Handoff evidence: RED/GREEN output, final command results, OpenSpec strict
  validation, reviewed diff, and the atomic task commit.
- Direct-execution fallback: all affected files implement one shared version
  contract, so splitting writes across agents would create conflicting version
  edits without useful parallelism.
