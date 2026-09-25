# IROIT application baseline

Current status (2026-09-24): `master` is
`42cde105c2c3f744f6a60bfb1984f842c3383cd2`; it contains merged S7 checkout
reliability. S8 payment-service extraction is implemented locally on the focused
branch with an independent schema/artifact/container and guarded copy. See the
[S8 runbook](payment-service.md) and [evidence handoff](payment-service-pr.md).
Remote checks for the eventual S8 commit are not yet evidence.

Current status (2026-09-22): S5 is merged at
`cddde6da41d32d3d37fae9a8eaa71cc4027cca73`, with Backend and Frontend success
verified on that exact commit (Actions run 35673771885). Identity owns sessions,
authentication, accounts and profiles. S6 notification extraction is implemented
and locally accepted in the working tree; see [current ownership/runbook](notification-service.md)
and [actual evidence and pending gates](notification-service-pr.md).
Older dated S4a/S4b/S5 status paragraphs below are historical. Proposed later-stage
architecture does not establish implemented behavior; the S6 runbook takes
precedence for current route, data, session and mail-delivery ownership.

S4a prerequisite verification (2026-09-20): freshly fetched `origin/master` and
local master identify `8d131049a8f8ed4b96f7d664eb57e3c52e934dc3`. Public GitHub API
confirms [S3 PR #20](https://github.com/DavidSubotinDS/SubotinMotors/pull/20) merged
to master at 2026-09-20T10:15:21Z. Its exact merged commit passed
[Backend](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35504550026/job/106062035971)
and [Frontend](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35504550026/job/106062036117).
GitHub CLI was unauthenticated; verification used public REST. Default branch
remains master. No applicable AGENTS.md was found; `.agents` was empty. Initial
working tree was clean on `feature/david.subotin_session-csrf-hardening`; it was
preserved. Created `feature/david.subotin_session-csrf-foundation` from verified
origin/master only after confirming these checks. Local and remote tag lists
were empty. No commit/stage/push/PR/merge/tag was performed. See
[S4a handoff](session-csrf-pr.md) for new local evidence, separate from S3 CI.

S3 prerequisite verification (2026-09-19): freshly fetched `origin/master`, local
master and the already-existing clean requested S3 branch all point to
`a08cb14d781464cc8a7008f7cb837e896d56f48e`, the merged S2 [PR #19](https://github.com/DavidSubotinDS/SubotinMotors/pull/19).
Its tree equals S2 branch `db53120`. [Merged-master run 35399306269](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35399306269)
passed Backend (including gateway build/container smoke) and Frontend (including
gateway browser E2E and cleanup). Public API confirms merge at 2026-09-18T21:57:21Z.
Remote HEAD/default branch is master. Local/remote tag lists are empty; no baseline
tag was created. Other branches, including unpushed architecture work, were preserved.
No applicable AGENTS.md was found. GitHub CLI remains unauthenticated; verification
used public REST. [S3 evidence](docker-compose-pr.md) is separate from this S2 CI.

S2 prerequisite verification (2026-09-14): freshly fetched master is
`35007b5eb71e602afa96e09f1c5f9d38e3f7406b`, the squash merge of S1 PR #18.
Its tree exactly matches S1 `fc59eb6`; an ancestry check alone would not detect
this squash merge. All six architecture files and both browser runbooks exist.
[Master CI run 34818110319](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/34818110319)
passed Backend and Frontend. The requested gateway branch already existed at
that exact master SHA and was reused with a clean working tree. Default branch
remains master. No local/remote tag was found; no baseline tag was created.
Protection inspection returned 401; required checks remain owner-reported.
The historical pre-architecture evidence and proposed tag below remain unchanged.

Inspection date: 2026-09-13. This records evidence before service extraction;
it does not claim completion of the IROIT course requirements.

## Verified Git and CI evidence

- The initial working tree was clean on `master`. No applicable `AGENTS.md`
  was found in the repository or its ancestor directories.
- `git fetch origin --prune` succeeded. Local `master`, `origin/master`, and
  the remote branch all identified `c74e283806d82fc0806e159fa259ce028a4db120`.
- GitHub's repository API confirmed `master` as the default branch.
- `git tag --list` and `git ls-remote --tags origin` found no tags.
- [PR #16](https://github.com/DavidSubotinDS/SubotinMotors/pull/16),
  `ci: add backend and frontend verification workflows`, was merged at
  `2026-09-13T18:36:15Z` with that exact merge SHA.
- The **push run on the merge commit**, [CI run 34775139488](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/34775139488),
  completed successfully. Both [Backend](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/34775139488/job/103771769482)
  and [Frontend](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/34775139488/job/103771769412)
  completed successfully. This is evidence for the merged commit, rather than
  only the PR branch or a synthetic PR merge.
- GitHub CLI was not authenticated. Verification used the public GitHub REST
  API (`repos/...`, `pulls/16`, `actions/runs?head_sha=...`, and the run's jobs).
- Required `Backend` and `Frontend` branch protection checks are user-reported;
  branch protection settings were not independently inspected.

The [existing stabilization record](stabilization-checklist.md) reports
96 backend and 12 frontend tests passing locally, plus successful builds.
Those counts were not re-run or re-counted in this documentation task. The CI
workflow was inspected: backend `clean verify`; frontend `npm ci`, tests, and
build; reports and build artifacts retained for seven days. CI success does
not establish a live Stripe purchase, MySQL deployment, or browser E2E pass.
Actuator is already a backend dependency and configuration exposes `health,info`;
complete metrics, tracing and operational dashboards are not implemented.

## Proposed annotated tag: owner executes

Use `iroit-baseline` for the stable React/Spring application **before** service
extraction. No tag was created or pushed by this task. These are PowerShell
commands; the explicit SHA keeps the tag correct even after architecture work
is merged. Review the two tag checks first; if the name already exists, inspect
it instead of replacing it or force-pushing it.

```powershell
Set-Location C:\Projects\SubotinMotors
git fetch origin --tags
git tag --list iroit-baseline
git ls-remote --tags origin refs/tags/iroit-baseline "refs/tags/iroit-baseline^{}"
git show --no-patch --format=fuller c74e283806d82fc0806e159fa259ce028a4db120
git tag -a iroit-baseline c74e283806d82fc0806e159fa259ce028a4db120 -m "IROIT baseline: stable React and Spring Boot application before service extraction; Backend and Frontend CI passed in run 34775139488."
git show --no-patch iroit-baseline
git push origin refs/tags/iroit-baseline
git ls-remote --tags origin refs/tags/iroit-baseline "refs/tags/iroit-baseline^{}"
```

The annotated tag object has its own SHA; the remote peeled `^{}` entry should
be `c74e283806d82fc0806e159fa259ce028a4db120`. Preserve this tag once published.
It is a source baseline, not a backup of local database contents.

## Architecture task boundary

Created `feature/david.subotin_microservices-architecture` from freshly fetched
`origin/master` at the SHA above. Only Markdown documentation under `docs/`
belongs to this task. No service extraction, dependency remediation, application
changes, commits, pushes, PRs, or merges are part of the performed work.

Start reading at [IROIT architecture](iroit-architecture.md). Owner commands
and suggested commit/PR text are in the [migration plan](iroit-migration-plan.md).
