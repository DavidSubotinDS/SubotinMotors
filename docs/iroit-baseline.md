# IROIT application baseline

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
