# Course specification and remaining work

Source: the Serbian project specification supplied by the owner in this task.
This checklist distinguishes implemented components from stages still planned.
Owner scope decision (2026-09-25): stop service extraction at S8 and complete
static analysis, monitoring/observability, reactive evidence and defence.
S8 merged at `5ee4cb8b5c0445b8ffded0577709475cd876af8e`; Backend and Frontend
checks succeeded on that commit. CD Local failed, so the deployed S8 revision
still needs verification after the environment update and successful rerun.

Update (observability branch): static-analysis PR #28 is merged at `fd7a3ca` with
successful exact-commit Backend/Frontend checks. CD Local was queued at inspection.
Private metrics export, dashboard/alert configuration and collector smoke checks
are implemented locally; see [current evidence and remaining gates](observability.md).
Safe HTTP traces and correlated request logs are implemented. Full runtime evidence and remaining owner gates are recorded in that handoff.

| Requirement | Current evidence / remaining work |
| --- | --- |
| At least four microservices, each with an endpoint, plus gateway | Four independently built/deployable application owners exist after S8: backend, identity, notification and payment. Each has endpoints; gateway is additional. Backend retains commerce and marketplace coupling. Frontend/MySQL/RabbitMQ are not counted as business services. |
| REST and message queue communication | REST already exists; S6 adds real RabbitMQ outbox/inbox delivery. See S6 tests for actual acceptance. |
| Reactive communication | Gateway uses WebFlux and an asynchronous WebClient identity exchange. Gateway ReactiveTraceTests demonstrate overlapping nonblocking session-exchange requests and propagated child traces. S9 checkout fan-out is deferred; do not claim it exists. |
| gRPC | Optional in the supplied specification; not required for acceptance. |
| Git platform, feature branches, PR history, stable default branch | GitHub and feature/PR workflow exist. Keep `master` as the established default equivalent of the rubric's `main`; owner commits, pushes and merges. Preserve PR history for defence. |
| Unit and end-to-end tests, all passing | Suites and required CI gates exist; every stage needs exact-revision green results. Local results do not establish remote CI success. |
| CI on push and PR, build/test/artifacts | Backend/Frontend cover both events, all four application builds plus gateway, tests, artifacts, containers and broker/MySQL/browser gates. Merged PMD/ESLint and Windows credential-script checks block those gates; this branch adds collector and real-stack telemetry assertions. |
| Docker per service, optimized images, ignores, env, local test, tags, CI builds | Independent multi-stage service Dockerfiles and Compose/CI builds exist. Local CD builds each release image with the exact Git commit SHA. Publishing to a registry remains optional for the selected local deployment. |
| Compose services/database/networks/env/health/persistence | Implemented; S6 adds a private broker volume and notification schema. Normal-data upgrade requires the documented guarded cutover. |
| Static analysis integrated into CI | Merged in PR #28, with successful exact-commit Backend/Frontend CI: PMD across all Java owners/gateway and ESLint across frontend/tooling; failures block Backend/Frontend and reports are uploaded. See [rules, scope and evidence](static-analysis.md). |
| Deployment, local or cloud | Local Compose deployment, guarded initialization, preserved-data upgrades and gateway smoke exist. S8 CD must be rerun successfully before claiming the payment revision is deployed. A public URL is required only if cloud is chosen. |
| Separate automatic CD after merge, deploy, post-deploy check | Implemented for local deployment: successful push CI on `master` triggers the separate `CD Local` workflow, exact-SHA image builds, serialized deployment and readiness/session/frontend checks. Owner-provided self-hosted runner and repository variables are required. |
| Logs, metrics, traces; latency, throughput, error rate; monitoring stack | Observability baseline implemented; consult the handoff for actual acceptance results: Micrometer/Prometheus metrics, Grafana dashboards and alerts, Tempo traces, Alloy/Loki logs, private management listeners, safe-field exporters, telemetry boundary tests and disposable collector/application overlay checks. Remote CI and owner-run final deployment evidence remain gates. |
| Demonstrate the complete DevOps workflow | Prepare repository/PR links and a rehearsed change → CI → merge → automatic CD → health/monitoring demonstration after the remaining stages. |

S9-S11 are deferred by explicit owner decision. Their chapters describe a future
architecture, not unfinished mandatory extraction for this delivery. The current branch adds the course observability baseline and defence guide.
The broader S12 asynchronous traces and operational dashboards remain deferred;
see the explicit scope distinction in the observability handoff. Local CD already exists. Public hosting, registry publication and gRPC
remain optional under the supplied specification. Real Stripe sandbox and SMTP
delivery remain unverified functional integrations, distinct from passing
simulated payment/mail tests.
