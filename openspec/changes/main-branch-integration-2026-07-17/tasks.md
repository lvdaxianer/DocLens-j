## Task Boundary And Agent Dispatch Plan

### Task 1: Prepare isolated integration workspace
- Module-oriented agent name: main-agent/git-integration
- Owned responsibility: inspect branch ancestry, create isolated worktree, and
  scaffold the OpenSpec change
- Allowed files or modules: `.worktrees/main-integration/**`,
  `openspec/changes/main-branch-integration-2026-07-17/**`
- Out-of-scope work: modifying application code, deleting branches, pushing to
  remote
- Dependencies: local git metadata, OpenSpec CLI
- Focused verification commands:
  - `git status --short --branch`
  - `git branch --no-merged main --format='%(refname:short)' | sort`
  - `git rev-list --left-right --count main...<branch>`
- Broader verification commands:
  - `git log --oneline --graph --decorate --all --simplify-by-decoration --branches`
- Handoff evidence: pending branch list, ancestor evidence, isolated worktree
  path
- Direct-execution fallback reason: this task edits git metadata and OpenSpec
  assets in one isolated worktree, so delegating would not improve safety

### Task 2: Integrate pending feature chain into the workflow branch
- Module-oriented agent name: main-agent/git-integration
- Owned responsibility: merge the effective pending feature branch into the
  integration branch and resolve conflicts if needed
- Allowed files or modules: git refs and files materialized inside
  `.worktrees/main-integration`
- Out-of-scope work: changing feature implementation beyond conflict resolution,
  deleting historical branches, pushing to remote
- Dependencies: completion of Task 1
- Focused verification commands:
  - `git merge --no-ff --no-commit <branch>` or `git merge --no-ff <branch>`
  - `git status --short --branch`
  - `git diff --stat main...HEAD`
- Broader verification commands:
  - `git log --oneline --graph --decorate --all --simplify-by-decoration --branches`
  - `git branch --contains <top-branch> --format='%(refname:short)'`
- Handoff evidence: merge result, conflict status, integrated file set
- Direct-execution fallback reason: the merge touches shared repository history
  and must stay serialized in a single isolated worktree

### Task 3: Update `main` from the verified integration branch
- Module-oriented agent name: main-agent/git-integration
- Owned responsibility: move `main` to the verified integrated history and
  record final evidence
- Allowed files or modules: git refs and files materialized inside
  `.worktrees/main-integration`
- Out-of-scope work: remote push, branch deletion, unrelated file cleanup
- Dependencies: completion of Task 2
- Focused verification commands:
  - `git checkout main`
  - `git merge --ff-only feat/main_整合非main分支` or a reviewed merge fallback
  - `git status --short --branch`
- Broader verification commands:
  - `git rev-list --left-right --count main...<branch>`
  - `git log --oneline --graph --decorate --all --simplify-by-decoration --branches`
- Handoff evidence: `main` tip commit, remaining non-merged branch list
- Direct-execution fallback reason: final ref update must remain a single-owner
  operation

## Tasks

- [x] 1. Prepare the isolated worktree and collect ancestry evidence for all
  local non-`main` branches.
- [x] 2. Merge the effective pending feature branch chain into
  `feat/main_整合非main分支` and verify the integrated result.
- [x] 3. Update `main` from the verified integration branch and capture final
  branch-status evidence.
