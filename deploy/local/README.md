# Local continuous deployment

This deployment targets one Windows machine running Docker Desktop. GitHub-hosted
runners cannot reach that machine, so `.github/workflows/cd-local.yml` uses a
repository-level self-hosted runner with the custom `autostrada-local` label.
After a successful `CI` push run on `master`, it checks out that exact commit,
builds seven SHA-tagged images locally, backs up all present owner schemas, updates the
dedicated `autostrada-local` Compose project and checks the public gateway origin.
No application service or database port other than the loopback gateway is exposed.

The deployment uses its own `autostrada-local_mysql-data` and
`autostrada-local_rabbitmq-data` volumes. It never uses or removes the default
development Compose project's volumes. Private configuration, backups, current
revision state and failure logs live outside the repository.

## One-time host setup

The credential generator and S8 environment updater support Windows PowerShell
5.1 and PowerShell 7. CI exercises both without Docker or permanent deployment
files. The CD workflow itself still requires PowerShell 7 (`shell: pwsh`).
For an existing deployment missing S8 credentials, run from the repository root:

```powershell
.\deploy\local\Update-LocalEnvironmentS8.ps1 -Path C:\AutostradaDeploy\autostrada.env
```

It only appends absent settings and is safe to rerun; it does not start containers.
An `IsPathFullyQualified` error came from using a pre-fix script with Windows
PowerShell 5.1. The corrected script also replaces unsupported cryptography APIs.
Do not recreate the environment with `New-LocalEnvironment.ps1` to fix an upgrade.

Prerequisites are Git, PowerShell 7, Docker Desktop with Linux containers, and a
repository-level GitHub Actions runner. Do not register the runner until the
initialization below succeeds; this prevents the first merged CD workflow from
trying to upgrade an uninitialized deployment.

After this change is merged, pull `master` and create host-only paths:

```powershell
cd C:\Projects\SubotinMotors
git switch master
git pull --ff-only origin master
pwsh ./deploy/local/New-LocalEnvironment.ps1 -Path C:\AutostradaDeploy\autostrada.env
$sha = git rev-parse HEAD
pwsh ./deploy/local/Initialize-Local.ps1 -Sha $sha -EnvFile C:\AutostradaDeploy\autostrada.env -StateRoot C:\AutostradaDeploy\state
```

Initialization asks once before creating the dedicated volumes. It deliberately
runs the guarded S5/S6 sequence: V1-V18, identity copy/parity, V19-V20,
notification copy/parity, then V21 and the complete stack. It refuses any existing
project volume. Open `http://localhost:8081` after it reports success.

In GitHub, open **Settings → Actions → Runners → New self-hosted runner**, choose
Windows x64 and run GitHub's generated commands from a dedicated directory such
as `C:\actions-runner`. Add the custom label `autostrada-local`; install/start it
as a Windows service under an account that can use Docker Desktop and read
`C:\AutostradaDeploy`. Set these repository variables under
**Settings → Secrets and variables → Actions → Variables**:

| Variable | Value |
| --- | --- |
| `LOCAL_DEPLOY_ENV_FILE` | `C:\AutostradaDeploy\autostrada.env` |
| `LOCAL_DEPLOY_STATE_ROOT` | `C:\AutostradaDeploy\state` |

Create the GitHub environment `local-production` and restrict its deployment
branch to `master`. A required reviewer would make deployment manual and therefore
should remain disabled for the course's automatic-CD requirement.

## Normal deployment and evidence

Every successful push CI run on `master` automatically queues one deployment.
The workflow verifies that the SHA is contained in `origin/master`. Manual reruns
can use **Actions → CD Local → Run workflow** with a full SHA already on master.
The current revision and private backups remain under the state root. Sanitized
status evidence is uploaded to the workflow run for 14 days; secrets and database
dumps are never uploaded.

The deploy refuses to proceed if MySQL is stopped because it cannot make a
pre-deploy backup. Start the existing deployment without rebuilding it, then
rerun the failed workflow:

```powershell
$env:DEPLOY_SHA = Get-Content C:\AutostradaDeploy\state\current-sha.txt
docker compose -p autostrada-local --env-file C:\AutostradaDeploy\autostrada.env -f compose.yaml -f deploy/local/compose.cd.yaml up -d mysql
```

## Failure, restore and rollback

Failed deployments retain containers, logs and the timestamped pre-deploy dump.
The script does not automatically restore images or databases. After identity,
role, reset, notification, outbox or broker writes, restoring an old image or dump
can re-enable stale security state or duplicate delivery. Diagnose the retained
logs, preserve all volumes, and prefer a corrected forward deployment.

For disaster recovery, stop public ingress and all writers, restore the three
schema dump into a new disposable project first, and verify Flyway histories,
row counts, identity/profile hashes, notification read state and outbox/dedupe
state. Restore the coordinated MySQL data, RabbitMQ volume and exact private
configuration together. A database-only or image-only rollback is not valid.

To stop the local deployment without deleting data:

```powershell
$env:DEPLOY_SHA = Get-Content C:\AutostradaDeploy\state\current-sha.txt
docker compose -p autostrada-local --env-file C:\AutostradaDeploy\autostrada.env -f compose.yaml -f deploy/local/compose.cd.yaml stop
```

Never run `down --volumes` for `autostrada-local` unless its retained deployment
data has been intentionally retired and separately verified.

