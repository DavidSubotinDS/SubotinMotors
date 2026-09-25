# Local observability: implementation and acceptance

The observability baseline is implemented on `feature/david.subotin_observability`
in `C:\Projects\SubotinMotors\target\observability-prep`. Local validation passed as
recorded below. Remote CI and permanent deployment remain separate owner gates.

Prerequisite: static-analysis PR #28 merged as
`fd7a3cab24240008e64b7d6a76d8072a564eb3e3`. Backend, Frontend and Deployment
environment scripts succeeded on that exact commit (CI run 36095748028).
CD Local run 36096923341 was queued at inspection; deployment is unverified.

## Implemented behavior

The four business owners (backend, identity, notification, payment) and gateway
export private Prometheus metrics, sanitized HTTP spans to Tempo, and curated
trace-correlated request logs through Alloy to Loki. Grafana provisions all three
datasources and a five-panel service-health dashboard. Only authenticated Grafana
is published, on loopback port 3001 by default. Collector APIs and management
port 9080 remain private. No Docker socket is mounted.

The dashboard shows throughput, HTTP 5xx error ratio, p95 latency, scrape status
and alerts. Health/scrape traffic is excluded. No traffic means no error-ratio
sample; missing telemetry is not healthy traffic. Demonstration alert defaults
are 5% errors or p95 above two seconds for five minutes with at least 0.1 requests
per second; these are not contractual service objectives. No external paging is
configured. Prometheus retains three days/512 MB, Loki and Tempo three days.
Application request logs rotate at 5 MB with a 10 MB/two-day cap per process.
Grafana uses bundled plugins from the pinned image; runtime plugin auto-updates
are disabled to keep its read-only filesystem and datasource registration stable.
Alloy offsets are ephemeral: restarting it may re-read retained log lines.

`compose.observability.yaml` enables the Spring profile and private listeners,
log volumes, internal telemetry network and `/livez`/`/readyz` probe paths.
Applications do not depend on collectors for startup. Without the overlay,
tracing is disabled and the existing health paths remain. Export has a bounded
one-second timeout. Spring-managed HTTP clients create/propagate child spans;
existing authentication, timeouts and no mutation replay are preserved.

Safe exporters retain trace relationships, service identity, span timing/kind,
HTTP method and numeric status. They discard URL/query, bodies, headers, arbitrary
attributes, events, links, status descriptions and tracestate. Dedicated logs
contain method, status, duration and trace/span IDs. Ordinary console diagnostics
remain local and are not shipped wholesale to Loki. Trace sampling is 100% for
this small local demonstration; revisit volume/cost before production scale.

Local CD opts in through `OBSERVABILITY_ENABLED=true` in the private env file.
`Enable-LocalObservability.ps1` generates a missing Grafana password without
changing application credentials. CD checks all five scrapes and Tempo/Loki
health after the public-origin smoke. No normal deployment or database was
changed during implementation.

## Actual evidence (2026-09-25)

- Independent Maven `verify -Pstatic-analysis`: backend 118, gateway 30,
  identity 44, notification 18, payment 11 tests; **221 passed**, production JARs
  and PMD passed. An obsolete payment-test report from an earlier fixture package
  remains in ignored target output; the latest Maven run reports 11/0/0.
- Frontend: ESLint, 30 tests and production build passed.
- Environment helper regression: PowerShell 7.6.5 and Windows PowerShell
  5.1.26100.9444 passed (process-scoped execution-policy bypass for the latter).
- Collector smoke `autostrada-observability-test-b8b0ed6ef311` passed rule tests,
  Grafana provisioning/authentication, real Prometheus query, synthetic Tempo
  ingestion and Alloy/Loki delivery. Owned resource cleanup verified. This proves
  collector wiring, not application telemetry.
- Final collector smoke `autostrada-observability-test-7b226e9cc219` passed after
  disabling Grafana runtime plugin updates. An intervening run exposed plugin
  registration failures against the read-only filesystem; its logs are retained.
  Final cleanup completed with no owned resources remaining.
- Full-stack run `autostrada-test-67bcea3923f97f5db3c22944b899fb80`: production
  image builds and S5/S6/S8 copy/parity, MySQL/broker delivery and security checks
  passed before a Grafana query failure. It did **not** pass full acceptance.
  Failure diagnostics retained; owned resource cleanup verified.
- Full application run `autostrada-test-4f721e315d3b788c4c4085eb60ff91a8` passed:
  production container builds, S5/S6/S8 data copy/parity, all five private scrapes,
  real gateway/backend trace, correlated Loki logs from all five processes, public
  metrics denial, collector outage/recovery, MySQL constraints/grants, broker
  delivery/recovery, preserved-volume restart, four-schema backup/restore and
  database outage/recovery. All **20 Chromium browser scenarios passed** through
  the actual Compose gateway against isolated MySQL in 2.7 minutes. Payments were
  simulated; this is not real Stripe or SMTP evidence.
- Final owned-resource cleanup passed: no owned containers, volumes or networks remain.
- Remote CI for this uncommitted branch and permanent owner-run deployment: unverified.

Logs are private ignored artifacts under `target/` and `compose-results/`.
No passwords, token values or private env files belong in the PR.

## Repeat validation

From this worktree, with Docker Desktop running:

```powershell
node infra/observability/smoke.mjs
node front/e2e/compose.mjs --integration-only --observability
npm --prefix front run test:compose -- --observability
.\deploy\local\Test-EnvironmentScripts.ps1
```

Each Compose runner uses a random project, credentials, schemas, ports and volumes;
it retains failure evidence and cleans only owned resources on success/failure.
The first command is collector-only. The second exercises production images and
MySQL. The third also runs browser regressions with a simulated payment provider.
H2 unit/integration tests are separate from these MySQL results.

## Scope and remaining limitations

The owner's course-focused scope defers S9–S11 extractions. This delivers the
rubric's service metrics/logs/HTTP traces and existing reactive WebClient evidence.
The older S12 architecture chapter proposes more: asynchronous RabbitMQ trace
correlation and dashboards for outbox/queue depth, stock holds, worker/connection
pressure and notification lag. Those remain future work; this is not completion
of every aspirational S12 bullet. RabbitMQ reliability/poison tests already exist,
but a continuous cross-message distributed trace is not implemented.

Real Stripe sandbox and SMTP delivery remain unverified. Test providers/log-mode
suppression are not evidence of either. Owner merge and exact-master green CI,
followed by successful local CD and a rehearsed defence, remain required.

The sections below contain the selective Git commands, merge, deployment and recovery instructions. Use the separate [defence guide in specification order](observability-defense.md).

Official references:

- [Prometheus rule tests](https://prometheus.io/docs/prometheus/latest/configuration/unit_testing_rules/)
- [Grafana provisioning](https://grafana.com/docs/grafana/latest/administration/provisioning/)
- [Spring Boot tracing and automatic HTTP propagation](https://docs.spring.io/spring-boot/3.5/reference/actuator/tracing.html)
- [Grafana plugin auto-update settings](https://grafana.com/docs/grafana/latest/setup-grafana/configure-grafana/#preinstall_auto_update)

## Owner Git commands

The current preparation worktree also contains ignored convenience files
`target/observability-stage-files.txt` (the exact file list below) and
`target/observability-pr-body.txt` (the suggested PR text). These are local aids,
not additional repository documentation. They contain no credentials.

Run in PowerShell 7. These commands are provided for the owner; the agent has not
staged, committed or pushed. Review the acceptance report first. Do not run `git
add .` from the original checkout. This exact list excludes private test artifacts.

```powershell
Set-Location C:\Projects\SubotinMotors\target\observability-prep
git status --short --branch
if ((git branch --show-current) -ne 'feature/david.subotin_observability') { throw 'Wrong branch' }
$files = @(
    '.github/workflows/ci.yml'
    'back/Dockerfile'
    'back/pom.xml'
    'back/src/main/java/lithan/autostrada/auctions/config/MetricsSecurity.java'
    'back/src/main/java/lithan/autostrada/auctions/config/SafeSpanExporter.java'
    'back/src/main/java/lithan/autostrada/auctions/config/SecurityConfig.java'
    'back/src/main/java/lithan/autostrada/auctions/config/TelemetryConfiguration.java'
    'back/src/main/java/lithan/autostrada/auctions/config/TelemetryRequestLog.java'
    'back/src/main/java/lithan/autostrada/auctions/identity/RemoteProfileClient.java'
    'back/src/main/java/lithan/autostrada/auctions/notification/NotificationClient.java'
    'back/src/main/java/lithan/autostrada/auctions/payment/RemotePaymentGateway.java'
    'back/src/main/resources/application-observability.properties'
    'back/src/main/resources/application.properties'
    'back/src/main/resources/logback-spring.xml'
    'back/src/test/java/lithan/autostrada/auctions/BusinessIdentityFixtures.java'
    'back/src/test/java/lithan/autostrada/auctions/identity/RemoteProfileClientTests.java'
    'back/src/test/java/lithan/autostrada/telemetry/MetricsBoundaryTests.java'
    'back/src/test/java/lithan/autostrada/telemetry/TracePrivacyTests.java'
    'compose.observability.yaml'
    'deploy/local/Common.ps1'
    'deploy/local/Enable-LocalObservability.ps1'
    'deploy/local/README.md'
    'deploy/local/Test-EnvironmentScripts.ps1'
    'docs/iroit-api-events.md'
    'docs/iroit-architecture.md'
    'docs/iroit-baseline.md'
    'docs/iroit-migration-plan.md'
    'docs/iroit-requirements-status.md'
    'docs/iroit-security.md'
    'docs/iroit-service-ownership.md'
    'docs/observability-defense.md'
    'docs/observability.md'
    'front/e2e/compose.mjs'
    'gateway/Dockerfile'
    'gateway/pom.xml'
    'gateway/src/main/java/lithan/autostrada/gateway/EdgeBoundary.java'
    'gateway/src/main/java/lithan/autostrada/gateway/IdentityBridge.java'
    'gateway/src/main/java/lithan/autostrada/gateway/SafeSpanExporter.java'
    'gateway/src/main/java/lithan/autostrada/gateway/TelemetryConfiguration.java'
    'gateway/src/main/java/lithan/autostrada/gateway/TelemetryRequestLog.java'
    'gateway/src/main/java/lithan/autostrada/gateway/UpstreamHealth.java'
    'gateway/src/main/java/lithan/autostrada/gateway/UpstreamTransport.java'
    'gateway/src/main/resources/application-observability.properties'
    'gateway/src/main/resources/application.properties'
    'gateway/src/main/resources/logback-spring.xml'
    'gateway/src/test/java/lithan/autostrada/gateway/MetricsBoundaryTests.java'
    'gateway/src/test/java/lithan/autostrada/gateway/ReactiveTraceTests.java'
    'gateway/src/test/java/lithan/autostrada/telemetry/TracePrivacyTests.java'
    'infra/observability/alloy/config.alloy'
    'infra/observability/compose.lab.yml'
    'infra/observability/grafana/dashboards/services.json'
    'infra/observability/grafana/provisioning/dashboards/dashboards.yml'
    'infra/observability/grafana/provisioning/datasources/datasources.yml'
    'infra/observability/loki/loki.yml'
    'infra/observability/prometheus/prometheus.yml'
    'infra/observability/prometheus/rules.test.yml'
    'infra/observability/prometheus/rules.yml'
    'infra/observability/smoke.mjs'
    'infra/observability/tempo/tempo.yml'
    'README.md'
    'services/identity-service/Dockerfile'
    'services/identity-service/pom.xml'
    'services/identity-service/src/main/java/lithan/autostrada/identity/config/MetricsSecurity.java'
    'services/identity-service/src/main/java/lithan/autostrada/identity/config/SafeSpanExporter.java'
    'services/identity-service/src/main/java/lithan/autostrada/identity/config/SecurityConfig.java'
    'services/identity-service/src/main/java/lithan/autostrada/identity/config/TelemetryConfiguration.java'
    'services/identity-service/src/main/java/lithan/autostrada/identity/config/TelemetryRequestLog.java'
    'services/identity-service/src/main/resources/application-observability.properties'
    'services/identity-service/src/main/resources/application.properties'
    'services/identity-service/src/main/resources/logback-spring.xml'
    'services/identity-service/src/test/java/lithan/autostrada/telemetry/MetricsBoundaryTests.java'
    'services/identity-service/src/test/java/lithan/autostrada/telemetry/TracePrivacyTests.java'
    'services/notification-service/Dockerfile'
    'services/notification-service/pom.xml'
    'services/notification-service/src/main/java/lithan/autostrada/notification/MetricsSecurity.java'
    'services/notification-service/src/main/java/lithan/autostrada/notification/SafeSpanExporter.java'
    'services/notification-service/src/main/java/lithan/autostrada/notification/SecurityConfig.java'
    'services/notification-service/src/main/java/lithan/autostrada/notification/TelemetryConfiguration.java'
    'services/notification-service/src/main/java/lithan/autostrada/notification/TelemetryRequestLog.java'
    'services/notification-service/src/main/resources/application-observability.properties'
    'services/notification-service/src/main/resources/application.properties'
    'services/notification-service/src/main/resources/logback-spring.xml'
    'services/notification-service/src/test/java/lithan/autostrada/telemetry/MetricsBoundaryTests.java'
    'services/notification-service/src/test/java/lithan/autostrada/telemetry/TracePrivacyTests.java'
    'services/payment-service/Dockerfile'
    'services/payment-service/pom.xml'
    'services/payment-service/src/main/java/lithan/autostrada/payment/MetricsSecurity.java'
    'services/payment-service/src/main/java/lithan/autostrada/payment/SafeSpanExporter.java'
    'services/payment-service/src/main/java/lithan/autostrada/payment/SecurityConfig.java'
    'services/payment-service/src/main/java/lithan/autostrada/payment/TelemetryConfiguration.java'
    'services/payment-service/src/main/java/lithan/autostrada/payment/TelemetryRequestLog.java'
    'services/payment-service/src/main/resources/application-observability.properties'
    'services/payment-service/src/main/resources/application.properties'
    'services/payment-service/src/main/resources/logback-spring.xml'
    'services/payment-service/src/test/java/lithan/autostrada/telemetry/MetricsBoundaryTests.java'
    'services/payment-service/src/test/java/lithan/autostrada/telemetry/TracePrivacyTests.java'
)
git add -- $files
git diff --cached --check
git diff --cached --stat
git diff --cached
```

After reviewing the staged diff:

```powershell
git commit -m "feat: add private observability and local deployment verification" -m "Instrument all four application services and the gateway with private Prometheus metrics, sanitized HTTP traces and correlated request logs. Provision Grafana, Tempo, Loki and Alloy with bounded retention and optional Compose deployment." -m "Gate CI on monitoring configuration, privacy boundaries, reactive propagation and real-stack telemetry checks. Add PowerShell-compatible private environment setup, post-deploy checks, recovery instructions and a specification-by-specification defence guide."
git push --set-upstream origin feature/david.subotin_observability
```

Open a PR into `master`; use the title/body in
[the merge and CD runbook](#owner-merge-cicd-and-startup-instructions). Wait for the latest
required checks before merging through GitHub. Do not merge locally or force push.
After the merge, follow that runbook to update master and verify the exact-SHA CD.

## Owner merge, CI/CD and startup instructions

Work exists in **C:\Projects\SubotinMotors\target\observability-prep**, on
`feature/david.subotin_observability`. The original checkout is still on the old
static-analysis branch. Do not commit from the wrong directory or delete this
preparation worktree. No commits, pushes, PRs or merges were performed by the agent.

## Before pushing

Read `docs/observability.md` for final local evidence and any pending
gates. A local pass permits submitting a PR, not bypassing its required CI checks.
The exact selective staging commands are in the Owner Git commands section above.
Review the staged diff before committing; credentials, `target/` and
`compose-results/` are excluded.

Suggested PR title: **feat: add private local observability and deployment checks**

Suggested PR body:

> Add optional Prometheus/Grafana/Tempo/Loki/Alloy monitoring for the four application
> owners and gateway. Keep metrics on private management listeners; expose
> trace-correlated request logs with bounded retention and sanitize spans before
> export. Use Spring-configured HTTP clients for child-span propagation while
> preserving existing timeouts, authentication and no mutation replay.
>
> Provision latency/throughput/server-error dashboards and alerts. Add collector
> smoke checks, private-listener/privacy/reactive tests, and real-stack telemetry
> assertions to required CI gates. Integrate the overlay with opt-in local CD,
> private credential setup and post-deploy scrape/datasource checks. Include a
> specification-by-specification defence guide and application walkthrough.
>
> Validation: see docs/observability.md for exact local results and
> limitations. Remote CI and the owner-run permanent deployment remain merge/
> deployment gates. No schema migration, service extraction or RabbitMQ tracing
> claim is introduced. Real Stripe sandbox and SMTP delivery remain unverified.

## Push, PR and merge

After the selective commands commit and push the feature branch:

1. Open GitHub → Pull requests → New pull request. Base `master`, compare
   `feature/david.subotin_observability`. Paste the title/body above.
2. Wait for **Backend** and **Frontend** to pass on the latest PR revision.
   Observability configuration and Deployment environment scripts must also pass;
   Backend depends on these. A previous commit's green check is not enough.
3. Review the diff, resolve any conflict on the feature branch, and rerun checks
   after changes. Do not merge locally to bypass the PR workflow.
4. Use GitHub's **Squash and merge** (or your established PR merge method).
5. Wait for push CI on the resulting master SHA to pass. CD Local then triggers
   automatically. A successful PR run alone does not trigger deployment.

Optional GitHub CLI commands, only after you have authenticated `gh`:

```powershell
gh pr checks --watch
gh pr merge --squash
```

Run these from the feature worktree after creating its PR. Do not add `--admin`.

## Prepare the local runner and private environment

Use PowerShell 7 (`$PSVersionTable.PSVersion`), as your normal Windows user with
Docker Desktop access. Keep the computer awake during CI-triggered deployment.
The existing initialized `autostrada-local` MySQL container must be running:
the deployment script refuses upgrades without its pre-deploy backup. Check
Docker Desktop or `docker ps`. If that deployment is stopped, start its existing
containers with Docker Desktop using the previously deployed configuration before
enabling this new overlay; do not initialize replacement volumes.

```powershell
Set-Location C:\Projects\SubotinMotors\target\observability-prep
docker version
docker compose version
.\deploy\local\Enable-LocalObservability.ps1 -Path C:\AutostradaDeploy\autostrada.env
```

The last script adds missing Grafana credentials and enables the overlay for the
**next deployment**. It preserves application passwords and does not initialize a
new database. Do not print the env file in a shared terminal/video. Grafana username
is `observer`; retrieve its generated `OBSERVABILITY_ADMIN_PASSWORD` privately in
an editor. Initial admin credentials are applied when Grafana's volume is first
created; changing the env later does not reset an existing Grafana password.

GitHub repository settings:

- Settings → Environments → **local-production**: reuse the existing environment.
- Ensure variables `LOCAL_DEPLOY_ENV_FILE=C:\AutostradaDeploy\autostrada.env` and
  `LOCAL_DEPLOY_STATE_ROOT=C:\AutostradaDeploy\state` exist at repository or
  environment scope. These are paths; the credentials stay on this computer.
- Settings → Actions → Runners: the runner must be online and have labels
  `self-hosted`, `Windows`, `X64`, `autostrada-local`.
- If environment protection requires a reviewer, approve the waiting deployment.

At the last inspection, master `fd7a3ca` had green Backend/Frontend and a **queued**
CD Local run (36096923341). Before starting the runner, cancel stale queued deploys
in Actions if you intend to deploy only the newly merged revision. Do not cancel
an active deployment in the middle of its guarded upgrade.

In a separate terminal start the already configured runner; do not register a
second one. Your earlier setup used a nested folder, so locate `run.cmd`:

```powershell
if (Test-Path C:\actions-runner\actions-runner\run.cmd) {
    Set-Location C:\actions-runner\actions-runner
} elseif (Test-Path C:\actions-runner\run.cmd) {
    Set-Location C:\actions-runner
} else {
    throw 'Locate the existing registered runner directory first.'
}
.\run.cmd
```

Leave that terminal running. If the runner is installed as a Windows service,
check that service instead; do not start a second listener for the same runner.
“Listening for Jobs” plus an Online/Idle runner in GitHub is the expected state.

If CD remains queued: inspect the run's waiting reason, exact labels, runner status,
environment approval and concurrency (another deployment may hold the slot).
Starting frontend/backend development terminals will not fix a queued Actions job.

## After the merge

First confirm the preparation worktree has no uncommitted changes. Then update the
original checkout without deleting any work:

```powershell
Set-Location C:\Projects\SubotinMotors
git status --short --branch
git fetch origin master
git switch master
git pull --ff-only origin master
```

If Git reports local changes or divergence, stop and preserve them; do not reset.
Do not rerun `New-LocalEnvironment.ps1` or `Initialize-Local.ps1` on an already
initialized deployment. Existing data stays in the `autostrada-local` volumes.

If automatic CD did not run, open Actions → **CD Local** → Run workflow → master,
and supply the full merged SHA whose master CI is green. This manual trigger
requires the same runner and variables. Prefer this to a second simultaneous manual
deploy. The workflow's manual path checks master ancestry; you must also verify
successful CI for the requested SHA.

Successful deployment evidence:

- CD Local ends green and uploads `local-deployment-<sha>`.
- `C:\AutostradaDeploy\state\current-sha.txt` matches the intended revision.
- `evidence\latest\deployment.json` records that same SHA and healthy status.
- Application and Grafana load, with five successful service scrapes.

## Starting an already deployed application before the defence

Docker Desktop first. Use the saved deployed SHA, not whatever branch happens to
be checked out. Run from the matching reviewed master revision:

```powershell
Set-Location C:\Projects\SubotinMotors
. .\deploy\local\Common.ps1
$env:DEPLOY_SHA = (Get-Content C:\AutostradaDeploy\state\current-sha.txt -Raw).Trim()
Invoke-Compose C:\AutostradaDeploy\autostrada.env @('up', '--detach', '--wait', '--wait-timeout', '300', '--no-build')
Test-PublicOrigin C:\AutostradaDeploy\autostrada.env
Invoke-Compose C:\AutostradaDeploy\autostrada.env @('ps')
```

If your source checkout differs from the deployed SHA, use CD to deploy the reviewed
master first instead of combining old images with new configuration. Start-up
commands above reuse release images; they do not build/tag uncommitted code.

Open application **http://localhost:8081** and Grafana **http://localhost:3001**
(or the private env's configured ports). The monitoring dashboard is
**Autostrada — service health**. No normal frontend/backend dev terminals are needed.
The self-hosted runner is needed for receiving CD jobs, not for keeping existing
containers alive.

## Safe failure demo and recovery

After sourcing Common.ps1 and setting DEPLOY_SHA as above:

```powershell
Invoke-Compose C:\AutostradaDeploy\autostrada.env @('stop', 'tempo')
Invoke-WebRequest http://localhost:8081/api/session
Invoke-Compose C:\AutostradaDeploy\autostrada.env @('start', 'tempo')
Test-PublicOrigin C:\AutostradaDeploy\autostrada.env
```

Show that the application still responds while trace storage is offline. Do not
leave Tempo stopped. Never use `down --volumes`, Docker prune or MySQL reset on
this deployment. Test/report failure details before claiming the demonstration
passed; not every outage immediately produces a 5xx metric.

## Rollout and rollback

Monitoring introduces no business schema migration. Deploy after green master CI;
CD retains the existing backup/cutover safeguards. If monitoring fails, retain the
private failure logs under the deployment state root. Do not publish secret files.
A green CI run is not proof that the self-hosted deployment succeeded.

To disable telemetry, set `OBSERVABILITY_ENABLED=false` privately in the env file,
then deploy the same or a corrected reviewed application revision through CD. The
base configuration returns to existing health paths and disabled tracing. The
`--remove-orphans` deployment step removes unused collector containers but does not
delete their named volumes or business data. Collector data is retained locally;
there is no need to restore any application DB for this rollback.

Do not roll back identity, password, roles, reset tokens, notification inbox/outbox,
broker or payment state to an older backup after new writes. Application rollback
requires schema/write compatibility review; a forward fix is preferred. A backup
is recovery evidence, not permission to re-enable stale credentials.
