# Phase 1: Git Repository Consolidation - Context

**Gathered:** 2026-01-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Merge the nested git repository's commit history into the root repository, then remove the nested .git folder. After this phase, git commands run from the project root should work correctly with a clean state.

</domain>

<decisions>
## Implementation Decisions

### History preservation
- Single squash commit: Combine all nested repo commits into one "import" commit
- No backup needed: Don't export or preserve the detailed nested history separately
- The nested repo's granular commit history is not valuable enough to preserve

### Merge approach
- Import method: Claude's discretion on whether to use git subtree, simple file move, or other approach
- Order of operations: Claude's discretion on when to delete nested .git (before or after moving files)
- Goal is simplicity: Whatever approach cleanly achieves the outcome

### Claude's Discretion
- Commit message for the squash import
- Technical approach to moving files (git mv, subtree, etc.)
- Order of git operations
- Author attribution on the import commit
- Any intermediate commits needed during the process

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches. The user trusts Claude to pick the simplest method that achieves:
1. Files accessible in root repo
2. Nested .git folder gone
3. Git commands work correctly from project root

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-git-repository-consolidation*
*Context gathered: 2026-01-20*
