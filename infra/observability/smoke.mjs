import { spawnSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import assert from 'node:assert/strict';

const directory = path.dirname(fileURLToPath(import.meta.url));
const project = `autostrada-observability-test-${randomBytes(6).toString('hex')}`;
const reports = path.resolve(directory, '../../target/observability', project);
mkdirSync(reports, { recursive: true });
const password = randomBytes(24).toString('hex');
const environment = {
  ...process.env,
  OBSERVABILITY_ADMIN_PASSWORD: password,
  // Docker chooses an unused loopback port; no permanent deployment port collision.
  OBSERVABILITY_GRAFANA_PORT: '0',
};
const compose = ['compose', '-p', project, '-f', path.join(directory, 'compose.lab.yml')];
function docker(args, { allowFailure = false, timeout = 180000 } = {}) {
  const result = spawnSync('docker', args, { env: environment, encoding: 'utf8', timeout });
  const output = `${result.stdout ?? ''}${result.stderr ?? ''}`.replaceAll(password, '[REDACTED]');
  if (!allowFailure && (result.error || result.status !== 0)) {
    throw new Error(`Docker command failed: ${args.slice(0, 2).join(' ')}\n${output}`);
  }
  return { output, status: result.status };
}
async function get(url, authenticated = true) {
  const response = await fetch(url, {
    headers: authenticated ? { Authorization: `Basic ${Buffer.from(`observer:${password}`).toString('base64')}` } : {},
    signal: AbortSignal.timeout(5000),
  });
  if (!response.ok) throw new Error(`HTTP ${response.status} ${await response.text()}`);
  return response.json();
}

let failure;
// Keep the CLI alive while fetch/AbortSignal timers are unreferenced by Node.
const keepAlive = setInterval(() => {}, 1000);
try {
  console.log(`Checking monitoring configuration (${project}).`);
  const root = path.resolve(directory, '../..');
  // Render only with generated fixtures, never the developer's deployment env file.
  for (const [, key] of readFileSync(path.join(root, 'compose.yaml'), 'utf8').matchAll(/\$\{([A-Z_]+):\?/g)) {
    environment[key] = 'configuration-fixture-only';
  }
  const emptyEnv = path.join(reports, 'empty.env');
  writeFileSync(emptyEnv, '');
  const rendered = JSON.parse(docker(['compose', '--env-file', emptyEnv, '-p', project,
    '-f', path.join(root, 'compose.yaml'), '-f', path.join(root, 'compose.observability.yaml'), 'config', '--format', 'json']).output);
  for (const service of ['backend', 'identity', 'notification', 'payment', 'gateway']) {
    assert.equal(rendered.services[service].environment.SPRING_PROFILES_INCLUDE, 'observability');
    assert.equal(rendered.services[service].environment.HEALTHCHECK_PATH, '/readyz');
    assert.ok(Object.hasOwn(rendered.services[service].networks, 'telemetry'));
    assert.ok(Object.hasOwn(rendered.services[service].networks, 'edge'));
    assert.ok(!(rendered.services[service].ports ?? []).some(port => Number(port.target) === 9080));
  }
  for (const args of [['check', 'config', '/etc/prometheus/prometheus.yml'], ['test', 'rules', 'rules.test.yml']]) {
    const result = docker(['run', '--rm', '--network', 'none', '--mount',
      `type=bind,source=${path.join(directory, 'prometheus')},target=/etc/prometheus,readonly`,
      '--workdir', '/etc/prometheus', '--entrypoint', '/bin/promtool', 'prom/prometheus:v3.14.0', ...args]);
    writeFileSync(path.join(reports, `${args[0]}.log`), result.output);
  }
  docker([...compose, 'config', '--quiet']);
  console.log('Starting the isolated Prometheus/Grafana stack.');
  docker([...compose, 'up', '-d'], { timeout: 300000 });
  const address = docker([...compose, 'port', 'grafana', '3000']).output.trim();
  assert.match(address, /^127\.0\.0\.1:\d+$/);
  const origin = `http://${address}`;
  const deadline = Date.now() + 90000;
  let ready = false;
  while (Date.now() < deadline) {
    try {
      ready = (await get(`${origin}/api/health`, false)).database === 'ok';
      if (ready) break;
    } catch { /* Bounded startup polling. */ }
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  assert.ok(ready, 'Grafana did not become ready');
  const dashboard = await get(`${origin}/api/dashboards/uid/autostrada-services`);
  assert.equal(dashboard.dashboard.panels.length, 5);
  const datasource = await get(`${origin}/api/datasources/uid/autostrada-prometheus`);
  assert.equal(datasource.url, 'http://prometheus:9090');
  const query = await get(`${origin}/api/datasources/proxy/uid/autostrada-prometheus/api/v1/query?query=vector(1)`);
  assert.equal(query.status, 'success');
  assert.equal(query.data.result[0].value[1], '1');
  const scrapeQuery = await get(`${origin}/api/datasources/proxy/uid/autostrada-prometheus/api/v1/query?query=${encodeURIComponent('up{job="autostrada"}')}`);
  assert.equal(scrapeQuery.status, 'success');
  for (const uid of ['autostrada-tempo', 'autostrada-loki']) {
    let healthy = false;
    const healthDeadline = Date.now() + 90000;
    while (Date.now() < healthDeadline) {
      try {
        healthy = (await get(`${origin}/api/datasources/uid/${uid}/health`)).status === 'OK';
        if (healthy) break;
      } catch { /* Collectors may need time to initialize their stores. */ }
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
    assert.ok(healthy, `${uid} did not become queryable`);
  }
  // Fixture-only traffic proves storage/query paths, not application instrumentation.
  const traceId = randomBytes(16).toString('hex');
  const spanId = randomBytes(8).toString('hex');
  const finished = BigInt(Date.now()) * 1000000n;
  const payload = JSON.stringify({ resourceSpans: [{ resource: { attributes: [{ key: 'service.name', value: { stringValue: 'telemetry-fixture' } }] },
    scopeSpans: [{ scope: { name: 'fixture' }, spans: [{ traceId, spanId, name: 'SERVER', kind: 2,
      startTimeUnixNano: String(finished - 10000000n), endTimeUnixNano: String(finished), status: { code: 1 } }] }] }] });
  docker(['run', '--rm', '--network', `${project}_telemetry`, '--entrypoint', '/bin/wget',
    'prom/prometheus:v3.14.0', '-qO-', '--header=Content-Type: application/json', '--post-data', payload, 'http://tempo:4318/v1/traces']);
  const line = `method=GET status=200 duration_ms=10 trace_id=${traceId} span_id=${spanId}`;
  docker(['run', '--rm', '--network', 'none', '--user', '0:0', '--cap-drop', 'ALL',
    '--mount', `type=volume,source=${project}_gateway-logs,target=/logs`, '--entrypoint', '/bin/sh',
    'prom/prometheus:v3.14.0', '-c', 'printf "%s\\n" "$1" >> /logs/requests.log', 'fixture', line]);
  let traceStored = false;
  let logStored = false;
  const storageDeadline = Date.now() + 90000;
  while (Date.now() < storageDeadline && (!traceStored || !logStored)) {
    try {
      const trace = await get(`${origin}/api/datasources/proxy/uid/autostrada-tempo/api/traces/${traceId}`);
      traceStored = JSON.stringify(trace).includes('telemetry-fixture');
    } catch { /* Wait for the newly ingested trace. */ }
    try {
      const logs = await get(`${origin}/api/datasources/proxy/uid/autostrada-loki/loki/api/v1/query_range?query=${encodeURIComponent(`{service="gateway"} |= "${traceId}"`)}&limit=10`);
      logStored = JSON.stringify(logs.data.result).includes(line);
    } catch { /* Wait for Alloy's file discovery and batch delivery. */ }
    if (!traceStored || !logStored) await new Promise(resolve => setTimeout(resolve, 1000));
  }
  assert.ok(traceStored, 'The trace fixture was not queryable in Tempo');
  assert.ok(logStored, 'The request log fixture did not reach Loki through Alloy');
  const unauthenticated = await fetch(`${origin}/api/dashboards/uid/autostrada-services`, { signal: AbortSignal.timeout(5000) });
  assert.equal(unauthenticated.status, 401);
  writeFileSync(path.join(reports, 'result.json'), JSON.stringify({ project, applicationOverlay: 'rendered with fixture credentials; private ports and retained networks verified', ruleTests: 'passed', dashboard: 'provisioned', datasource: 'query succeeded', traceFixture: 'stored and queried', logFixture: 'shipped by Alloy and queried', anonymousDashboard: 'denied', applicationTelemetry: 'not connected' }, null, 2));
  console.log('Rules, Grafana provisioning, metrics query, trace/log ingestion and authentication checks passed.');
} catch (error) {
  failure = error;
} finally {
  const logs = docker([...compose, 'logs', '--no-color'], { allowFailure: true });
  writeFileSync(path.join(reports, 'compose.log'), logs.output);
  const cleanup = docker([...compose, 'down', '--volumes', '--remove-orphans'], { allowFailure: true });
  writeFileSync(path.join(reports, 'cleanup.log'), cleanup.output);
  if (cleanup.status !== 0) failure ??= new Error(`Cleanup failed for ${project}; see ${reports}`);
  for (const kind of ['container', 'volume', 'network']) {
    const remaining = docker([kind, 'ls', ...(kind === 'container' ? ['--all'] : []), '--filter', `label=com.docker.compose.project=${project}`, '--format', kind === 'volume' ? '{{.Name}}' : '{{.ID}}'], { allowFailure: true });
    if (remaining.status !== 0 || remaining.output.trim()) failure ??= new Error(`Owned ${kind} resources remain or cleanup could not be verified`);
  }
  console.log(`Logs retained at ${reports}`);
  clearInterval(keepAlive);
}
if (failure) {
  console.error(failure.message.replaceAll(password, '[REDACTED]'));
  process.exitCode = 1;
} else {
  console.log('Verified removal of all owned containers, volumes and networks.');
}
