## Context

The repository currently has uncommitted documentation edits on the active
feature branch worktree, so merging directly in place would risk mixing
unrelated work into the branch integration. `main` is behind a linear feature
branch chain that contains the PostgreSQL unification, container packaging, and
local runtime updates.

This integration is operational rather than net-new feature development: the
goal is to move already-completed branch work onto `main` with a verifiable and
reversible procedure.

## Goals / Non-Goals

**Goals:**
- Integrate all local non-`main` code that is not already reachable from `main`
  into `main`
- Keep the current worktree's uncommitted documentation edit untouched
- Produce objective evidence showing which branches needed integration and which
  were already ancestors of `main`
- Land the integration in a way that leaves `main` ready for follow-up push or
  release work

**Non-Goals:**
- Rework or rewrite the integrated PostgreSQL/container implementation
- Clean up or delete historical local branches as part of this task
- Push `main` to `origin` without an explicit user request

## Decisions

### Use an isolated git worktree rooted from `main`
Using `.worktrees/main-integration` avoids stashing or editing the current dirty
tree. This keeps `docs/permission-configuration_zh.md` and any other local work
out of the integration path.

Alternative considered:
- Merge in the current worktree after stashing. Rejected because it would
  temporarily move or risk the user's in-progress documentation changes.

### Integrate the topmost pending feature branch instead of replaying redundant ancestors
Branch ancestry checks show that
`feat/feature/feature-main-统一PostgreSQL-容器化部署_Docker本地运行时`
already contains `feature/feature-main-统一PostgreSQL-容器化部署` and
`feature/main-统一PostgreSQL`. Merging the topmost branch captures the full
pending chain while keeping history simpler.

Alternative considered:
- Merge each pending branch separately. Rejected because the lower two branches
  are strict ancestors of the top branch and would add redundant merge steps
  without changing the final tree.

### Update `main` only after integration verification succeeds
The workflow branch `feat/main_整合非main分支` is used as the planning and
verification surface. After merge verification passes, `main` is fast-forwarded
or merged from this verified branch in the isolated worktree.

Alternative considered:
- Merge directly into checked-out `main`. Rejected because it removes the
  intermediate verification checkpoint required by the workflow.

## Risks / Trade-offs

- [History surprises from old local branches] -> Verify `git rev-list
  --left-right --count main...<branch>` for every non-`main` branch before the
  final update to distinguish ancestors from real pending work.
- [Merge conflicts while integrating the top feature branch] -> Resolve inside
  the isolated worktree and keep the original worktree untouched.
- [Workflow overhead for an operational merge] -> Keep the OpenSpec assets
  compact and focused on integration evidence rather than re-specifying the
  underlying feature implementation.

## Migration Plan

1. Create the isolated worktree and integration branch from `main`.
2. Create the OpenSpec change assets that describe the integration intent.
3. Merge the top pending feature branch into the integration branch.
4. Verify status, branch ancestry, and commit graph.
5. Update `main` from the verified integration branch inside the isolated
   worktree.
6. Re-run status and graph verification on `main`.

Rollback:
- If verification fails before `main` is updated, discard the integration
  branch or worktree.
- If `main` receives a bad merge locally, revert or reset should be decided
  explicitly by the user before any destructive action.

## Open Questions

- None at this stage; the pending branch chain and target branch are both
  explicit.
