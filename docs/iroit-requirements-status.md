# Course specification and remaining work

Source: the Serbian project specification supplied by the owner in this task.
This checklist distinguishes implemented components from stages still planned.
S6 remains subject to [its acceptance record](notification-service-pr.md).

| Requirement | Current evidence / remaining work |
| --- | --- |
| At least four microservices, each with an endpoint, plus gateway | Backend, identity and notification provide three application owners at S6. Gateway exists separately. Do not count frontend/MySQL/RabbitMQ toward four business services; later extraction remains required. |
| REST and message queue communication | REST already exists; S6 adds real RabbitMQ outbox/inbox delivery. See S6 tests for actual acceptance. |
| Reactive communication | Reactive gateway transport exists; the documented application-level reactive checkout interaction is S9. Demonstrate that interaction rather than assuming gateway internals satisfy the rubric. |
| gRPC | Optional in the supplied specification; not required for acceptance. |
| Git platform, feature branches, PR history, stable default branch | GitHub and feature/PR workflow exist. Keep `master` as the established default equivalent of the rubric's `main`; owner commits, pushes and merges. Preserve PR history for defence. |
| Unit and end-to-end tests, all passing | Suites and required CI gates exist; every stage needs exact-revision green results. Local results do not establish remote CI success. |
| CI on push and PR, build/test/artifacts | Existing Backend/Frontend required jobs cover both events, service builds, tests and artifacts. S6 adds notification build/reports and broker/MySQL/browser gates. |
| Docker per service, optimized images, ignores, env, local test, tags, CI builds | Independent multi-stage service Dockerfiles and Compose/CI builds exist. Local CD builds each release image with the exact Git commit SHA. Publishing to a registry remains optional for the selected local deployment. |
| Compose services/database/networks/env/health/persistence | Implemented; S6 adds a private broker volume and notification schema. Normal-data upgrade requires the documented guarded cutover. |
| Static analysis integrated into CI | Dedicated lint/static-analysis coverage and its quality gate still need implementation/verification; passing compiler/unit tests alone does not establish this requirement. |
| Deployment, local or cloud | A dedicated local Compose deployment, guarded clean initialization, preserved-data backup/upgrade path and public-origin smoke are implemented. The owner must configure the documented host runner after merge. A public URL is required only if cloud is chosen. |
| Separate automatic CD after merge, deploy, post-deploy check | Implemented for local deployment: successful push CI on `master` triggers the separate `CD Local` workflow, exact-SHA image builds, serialized deployment and readiness/session/frontend checks. Owner-provided self-hosted runner and repository variables are required. |
| Logs, metrics, traces; latency, throughput, error rate; monitoring stack | Service logs and partial instrumentation exist. Complete metrics export, dashboards, trace collection and cross-service demonstration in S12. S6 correlation logs are not proof of a complete tracing stack. |
| Demonstrate the complete DevOps workflow | Prepare repository/PR links and a rehearsed change → CI → merge → automatic CD → health/monitoring demonstration after the remaining stages. |

The migration plan remains S7 durable checkout preparation, S8 payment extraction,
S9 commerce/reactive interaction, S10 marketplace extraction, followed by the
documented cleanup and observability stages. The local part of S13 CD is now
implemented ahead of those stages; public hosting remains optional and is not
configured. Do not add cloud infrastructure solely for the rubric: local
deployment and monitoring are explicitly permitted.
