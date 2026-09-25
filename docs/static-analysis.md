# Static analysis and Windows credential-script compatibility

This branch combines the owner's S8 environment-script fix with the next course
requirement. No further service extraction is included. The base is merged S8
`5ee4cb8b5c0445b8ffded0577709475cd876af8e`, with exact-commit Backend/Frontend
success. Its CD deployment failed; that remains an operational follow-up.

## Quality gates

- Java: Maven PMD Plugin 3.28.0 (PMD 7.17.0), using
  `quality/pmd-ruleset.xml` across backend, gateway, identity, notification and
  payment production Java. Nineteen selected rules cover likely defects, dead
  code, unsafe numeric/null/string operations, concurrency mistakes, hardcoded
  cryptographic keys and IVs.
- Java analysis is enabled by the `static-analysis` Maven profile and bound to
  `verify`. CI enables it for each independent build in required `Backend`.
  Service Docker builds remain independent; they do not need a shared parent
  POM, ruleset copy or domain library in their build contexts.
- One reviewed exception remains visible in `back/target/pmd.xml`:
  `CheckResultSet` in applied V21. That migration stores `next()` in a boolean
  and checks it before using the table. Its checksum-protected source is
  unchanged. The class/rule exception is documented in
  `quality/pmd-reviewed-exceptions.properties`; no general baseline exists.
- Frontend: pinned ESLint 10, recommended JavaScript rules, JSX reference
  checking, plus dynamic-code and raw-HTML prohibitions. Checks cover React
  source, tests, configuration and browser harness. Generated bundles/reports
  and dependencies are excluded; Node globals are limited to tooling files.
  All warnings and errors fail required `Frontend`.
- Reports are retained as `java-static-analysis` (PMD XML) and
  `frontend-static-analysis` (ESLint JSON), including failed analyses.
- Windows host credential tests run on a GitHub-hosted Windows runner in both
  5.1 and 7. A failure explicitly fails Backend instead of allowing it to pass
  through a skipped dependency. No self-hosted deployment or Docker access is
  involved.

These checks do not prove the absence of vulnerabilities and do not replace
authorization tests, dependency auditing or manual review. Java test fixtures
are excluded from PMD because they deliberately contain simulated keys and
adversarial payloads. Existing unit/contract/browser suites continue to gate CI.

## Commands

For any Java module (substitute its directory):

```powershell
Set-Location C:\Projects\SubotinMotors\back
.\mvnw.cmd --batch-mode --no-transfer-progress -Pstatic-analysis pmd:check
# Build, test and run analysis together:
.\mvnw.cmd --batch-mode --no-transfer-progress -Pstatic-analysis verify
```

Frontend uses Node 22.13 or later (CI selects Node 22):

```powershell
Set-Location C:\Projects\SubotinMotors\front
npm ci
npm run lint
npm run lint:ci
```

The environment-script regression creates and removes one uniquely named
temporary fixture and checks preservation, idempotency, WhatIf, path validation,
256-bit random secrets and 2048-bit RSA generation:

```powershell
Set-Location C:\Projects\SubotinMotors
powershell.exe -NoProfile -File deploy/local/Test-EnvironmentScripts.ps1
pwsh -NoProfile -File deploy/local/Test-EnvironmentScripts.ps1
```

## Maintainer references

- [PMD Maven verification](https://maven.apache.org/plugins/maven-pmd-plugin/check-mojo.html)
- [PMD security rules](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_security.html)
- [ESLint 10 JSX reference handling](https://eslint.org/docs/latest/use/migrate-to-10.0.0)
- [ESLint configuration](https://eslint.org/docs/latest/use/configure/configuration-files)

Actual local results and owner commands are recorded in
[the focused handoff](static-analysis-pr.md). Remote checks for this branch
remain unverified until the owner pushes it.
