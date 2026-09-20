import { spawn } from 'node:child_process';
import { mkdir, writeFile, readFile } from 'node:fs/promises';
import { createWriteStream } from 'node:fs';
import { randomBytes } from 'node:crypto';
import { createServer } from 'node:net';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';

const root = fileURLToPath(new URL('../../', import.meta.url));
const id = randomBytes(16).toString('hex');
const project = `autostrada-test-${id}`;
const directory = resolve(root, 'compose-results', project);
const overlay = resolve(directory, 'override.json');
const emptyEnv = resolve(directory, 'empty.env');
const token = randomBytes(32).toString('hex');
const schema = `e2e_${id}`;
const browser = !process.argv.includes('--integration-only');
const env = Object.fromEntries(Object.entries(process.env).filter(([key]) =>
  !/^(COMPOSE_|MYSQL_|DB_|E2E_|SPRING_|STRIPE_|APP_|SMTP_|PAYMENTS_|AUCTION_|VITE_|GATEWAY_|PUBLIC_URL$|SESSION_|SERVER_|MANAGEMENT_|LOGGING_|JAVA_TOOL_OPTIONS$|JDK_JAVA_OPTIONS$|_JAVA_OPTIONS$)/i.test(key)));
Object.assign(env, { MYSQL_DATABASE: schema, MYSQL_USER: 'e2e', MYSQL_PASSWORD: randomBytes(32).toString('hex'),
  MYSQL_ROOT_PASSWORD: randomBytes(32).toString('hex'), APP_DEMO_DATA_ACK: 'I_ACCEPT_EXISTING_DEMO_DATA',
  E2E_CONTROL_TOKEN: token, TZ: 'UTC' });
env.E2E_REPORT_ROOT = resolve(directory, 'browser');
let cleanupPromise;
let sequence = 0;
const children = new Set();
const evidence = [];
let config;
let owned = false;

function execute(command, args, { input, allowFailure = false, timeout = 900000, cwd = root, log = true } = {}) {
  return new Promise((done, reject) => {
    const file = log ? createWriteStream(resolve(directory, `${String(++sequence).padStart(3, '0')}-${command === 'docker' ? 'docker' : 'browser'}.log`)) : null;
    const child = spawn(command, args, { cwd, env, windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] });
    // Decode across buffer boundaries so SQL backups preserve multibyte text.
    child.stdout.setEncoding('utf8');
    child.stderr.setEncoding('utf8');
    children.add(child);
    let stdout = '', stderr = '';
    const timer = setTimeout(() => { child.kill(); }, timeout);
    child.stdout.on('data', chunk => { stdout += chunk; file?.write(chunk); });
    child.stderr.on('data', chunk => { stderr += chunk; file?.write(chunk); });
    child.once('error', reject);
    child.once('close', code => {
      clearTimeout(timer); children.delete(child); file?.end();
      if (code !== 0 && !allowFailure) reject(new Error(`${command} ${args.slice(0, 1)} exited ${code}; ${stderr.slice(-3500)}`));
      else done({ code, stdout, stderr });
    });
    child.stdin.on('error', () => {});
    child.stdin.end(input);
  });
}
const compose = (args, options) => execute('docker', ['compose', '--project-name', project, '--env-file', emptyEnv,
  '-f', resolve(root, 'compose.yaml'), '-f', overlay, ...args], options);
async function freePort() {
  const server = createServer();
  await new Promise((done, reject) => { server.once('error', reject); server.listen(0, '127.0.0.1', done); });
  const port = server.address().port;
  await new Promise(done => server.close(done));
  return port;
}
async function saveConfig() { await writeFile(overlay, JSON.stringify(config, null, 2)); }
async function sql(statement, { rootUser = false, allowFailure = false } = {}) {
  return compose(['exec', '-T', 'mysql', 'sh', '-c', rootUser
    ? 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --skip-column-names "$MYSQL_DATABASE"'
    : 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u"$MYSQL_USER" --batch --skip-column-names "$MYSQL_DATABASE"'],
  { input: statement, allowFailure, log: false, timeout: 20000 });
}
async function value(statement) { return (await sql(statement)).stdout.trim(); }
async function checkHttp(path, status = 200, options = {}) {
  const response = await fetch(env.PUBLIC_URL + path, { redirect: 'manual', signal: AbortSignal.timeout(10000), ...options });
  assert.equal(response.status, status, `${path}: ${await response.clone().text()}`);
  assert.match(response.headers.get('x-request-id') || '', /^[a-f0-9-]{36}$/);
  return response;
}
async function record(message) { evidence.push(message); console.log(message); await writeFile(resolve(directory, 'evidence.json'), JSON.stringify({ project, evidence }, null, 2)); }
async function cleanup() {
  if (!owned) return;
  if (cleanupPromise) return cleanupPromise;
  cleanupPromise = (async () => {
    await compose(['logs', '--no-color'], { allowFailure: true, timeout: 30000 });
    await compose(['ps', '--all'], { allowFailure: true, timeout: 30000 });
    // Only a newly generated, collision-checked project is ever passed to down -v.
    await compose(['down', '--volumes', '--remove-orphans', '--rmi', 'local', '--timeout', '20'], { timeout: 90000 });
    for (const kind of ['container', 'volume', 'network']) {
      const result = await execute('docker', [kind, 'ls', ...(kind === 'container' ? ['-a'] : []), '--quiet',
        '--filter', `label=com.docker.compose.project=${project}`]);
      assert.equal(result.stdout.trim(), '', `Owned ${kind} resources remain`);
    }
    await record('Cleanup verified: no owned containers, volumes or networks remain.');
  })();
  return cleanupPromise;
}
for (const [signal, code] of [['SIGINT', 130], ['SIGTERM', 143]]) process.once(signal, async () => {
  for (const child of children) child.kill();
  try { await cleanup(); } finally { process.exit(code); }
});

try {
  await mkdir(directory, { recursive: true });
  await writeFile(emptyEnv, '');
  const port = await freePort();
  env.GATEWAY_PORT = String(port); env.PUBLIC_URL = `http://127.0.0.1:${port}`; env.E2E_GATEWAY_URL = env.PUBLIC_URL;
  config = { services: { backend: { environment: { AUCTION_NOTIFICATION_SCHEDULING: 'false' } } } };
  await saveConfig();
  // Refuse a collision before creating anything, even though the name has 128 random bits.
  for (const kind of ['container', 'volume', 'network']) {
    const result = await execute('docker', [kind, 'ls', ...(kind === 'container' ? ['-a'] : []), '--quiet',
      '--filter', `label=com.docker.compose.project=${project}`]);
    assert.equal(result.stdout.trim(), '', 'Project collision; refusing reuse');
  }
  owned = true;
  await writeFile(resolve(directory, 'project.json'), JSON.stringify({ project, port, schema }));
  await compose(['config', '--quiet']);
  console.log(`Building production images for ${project}; logs: ${directory}`);
  await compose(['build']);
  const seedGuard = await compose(['run', '--rm', '--no-deps', '-e', 'APP_DEMO_DATA_ACK=', 'backend'], { allowFailure: true });
  assert.equal(seedGuard.code, 1, 'Production startup must refuse missing demo acknowledgement');
  assert.match(seedGuard.stderr + seedGuard.stdout, /I_ACCEPT_EXISTING_DEMO_DATA/);
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240'], { timeout: 300000 });
  await compose(['images', '--format', 'json']);
  assert.equal(await value('SELECT COUNT(*) FROM flyway_schema_history WHERE success=1 AND version BETWEEN 1 AND 18'), '18');
  assert.equal(await value('SELECT COUNT(*) FROM flyway_schema_history WHERE success=0'), '0');
  await writeFile(resolve(directory, 'migrations.tsv'), (await sql('SELECT version,script,checksum,success FROM flyway_schema_history ORDER BY installed_rank')).stdout);
  for (const service of ['backend', 'frontend', 'gateway']) {
    assert.notEqual((await compose(['exec', '-T', service, 'id', '-u'])).stdout.trim(), '0');
  }
  const rendered = JSON.parse((await compose(['config', '--format', 'json'], { log: false })).stdout);
  for (const service of ['mysql', 'backend', 'frontend']) assert.ok(!rendered.services[service].ports?.length, `${service} unexpectedly published`);
  await checkHttp('/actuator/health/readiness');
  await checkHttp('/auctions/1');
  await checkHttp('/api/session');
  await checkHttp('/__e2e/ready', 404);
  await checkHttp('/webhooks/stripe/extra', 404, { method: 'POST' });
  await checkHttp('/webhooks/stripe', 404);
  await checkHttp('/assets/missing.js', 404);
  const anonymous = await checkHttp('/api/csrf');
  assert.equal(anonymous.headers.get('cache-control'), 'no-store');
  const anonymousCookie = anonymous.headers.get('set-cookie').split(';')[0];
  const anonymousToken = (await anonymous.json()).token;
  await checkHttp('/api/auth/login', 403, { method: 'POST', headers: { Cookie: anonymousCookie,
    'Content-Type': 'application/json' }, body: '{}' });
  const login = await checkHttp('/api/auth/login', 200, { method: 'POST',
    headers: { 'Content-Type': 'application/json', Cookie: anonymousCookie, 'X-CSRF-TOKEN': anonymousToken },
    body: JSON.stringify({ username: 'demo_bidder', password: 'demo123' }) });
  const cookie = login.headers.get('set-cookie');
  assert.match(cookie, /JSESSIONID=/); assert.match(cookie, /HttpOnly/i); assert.match(cookie, /SameSite=Lax/i);
  assert.match(cookie, /Path=\//i); assert.doesNotMatch(cookie, /Domain=|;\s*Secure/i);
  assert.ok(cookie.split(';')[0] !== anonymousCookie, 'Login must rotate the anonymous session');
  const oldSession = await checkHttp('/api/session', 200, { headers: { Cookie: anonymousCookie } });
  assert.equal((await oldSession.json()).authenticated, false);
  const signedInCookie = cookie.split(';')[0];
  await checkHttp('/api/auth/logout', 403, { method: 'POST', headers: { Cookie: signedInCookie, 'X-CSRF-TOKEN': anonymousToken } });
  const freshCsrf = await checkHttp('/api/csrf', 200, { headers: { Cookie: signedInCookie } });
  const signedInToken = (await freshCsrf.json()).token;
  const cors = await checkHttp('/api/session', 200, { method: 'OPTIONS',
    headers: { Origin: env.PUBLIC_URL, 'Access-Control-Request-Method': 'GET' } });
  // Same-origin requests need no CORS response headers. Compose permits no
  // additional browser origins; credentialed cross-origin behavior is covered by gateway tests.
  assert.equal(cors.headers.get('access-control-allow-origin'), null);
  assert.equal(cors.headers.get('access-control-allow-credentials'), null);
  await checkHttp('/api/session', 403, { headers: { Origin: 'https://untrusted.invalid' } });
  const redirect = await checkHttp('/cars', 302, { headers: { 'X-Forwarded-Host': 'untrusted.invalid', 'X-Forwarded-Proto': 'https' } });
  assert.equal(new URL(redirect.headers.get('location')).origin, env.PUBLIC_URL);
  await checkHttp('/api/auth/password-reset', 200, { method: 'POST', headers: { 'Content-Type': 'application/json',
    Cookie: signedInCookie, 'X-CSRF-TOKEN': signedInToken },
    body: JSON.stringify({ identifier: 'demo_bidder' }) });
  const resetLogs = (await compose(['logs', '--no-color', 'backend'], { log: false })).stdout;
  assert.ok(!/reset-password\?token=/.test(resetLogs), 'Reset links must not enter application logs');
  assert.equal(await value('SELECT COUNT(*) FROM tb_password_reset_token'), '1');
  await checkHttp('/api/auth/logout', 200, { method: 'POST', headers: { Cookie: signedInCookie, 'X-CSRF-TOKEN': signedInToken } });
  await record('Production images started non-root; private service ports, SPA/API/exact webhook routing and all 18 MySQL migrations verified.');
  await record('Production cookie attributes, login rotation/old-session rejection, CSRF, logout, CORS, trusted redirects and reset request without secret logging verified.');

  // Real MySQL checks: grants, unique/FK/CHECK enforcement, and a competing row lock.
  assert.notEqual((await sql('SELECT * FROM mysql.user', { allowFailure: true })).code, 0);
  for (const query of ["INSERT INTO tb_role(role,id_user) VALUES ('ROLE_USER',999999)",
    "INSERT INTO tb_user(username,password,email) SELECT username,password,email FROM tb_user LIMIT 1",
    'UPDATE tb_car_part SET stock_quantity=-1 WHERE id_part=1']) {
    assert.notEqual((await sql(query, { allowFailure: true })).code, 0, 'MySQL constraint did not reject invalid data');
  }
  // Named-lock handshake establishes that the holder owns the row before contender runs.
  const holder = sql("START TRANSACTION; SELECT id_part FROM tb_car_part WHERE id_part=1 FOR UPDATE; SELECT GET_LOCK('s3_row_locked',0); DO SLEEP(6); ROLLBACK; SELECT RELEASE_LOCK('s3_row_locked');");
  // Observe an early failure immediately; the await below still propagates it.
  holder.catch(() => {});
  const deadline = Date.now() + 10000;
  while (await value("SELECT IS_USED_LOCK('s3_row_locked') IS NOT NULL") !== '1') {
    assert.ok(Date.now() < deadline, 'Lock holder did not start');
    await new Promise(done => setTimeout(done, 100));
  }
  const contention = await sql('SET innodb_lock_wait_timeout=1; UPDATE tb_car_part SET stock_quantity=stock_quantity+1 WHERE id_part=1', { allowFailure: true });
  assert.notEqual(contention.code, 0); assert.match(contention.stderr, /1205/); await holder;
  await record('MySQL schema-only grants, unique/FK/CHECK constraints and row-lock contention verified.');

  await sql("UPDATE tb_user_profile SET about='S3 backup sentinel' WHERE id_profile=1");
  const snapshotQuery = "SELECT id_user,username,email,password FROM tb_user ORDER BY id_user; SELECT id_profile,about FROM tb_user_profile ORDER BY id_profile; SELECT version,checksum FROM flyway_schema_history ORDER BY installed_rank; SELECT id_picture,SHA2(image,256) FROM tb_car_gallery_picture ORDER BY id_picture";
  const snapshot = await value(snapshotQuery);
  const dump = await compose(['exec', '-T', 'mysql', 'sh', '-c',
    'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump -u"$MYSQL_USER" --single-transaction --no-tablespaces --set-gtid-purged=OFF --hex-blob "$MYSQL_DATABASE"'], { log: false });
  await writeFile(resolve(directory, 'backup.sql'), dump.stdout);
  await compose(['down', '--timeout', '20']); // Intentionally preserve volume.
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240'], { timeout: 300000 });
  assert.equal(await value(snapshotQuery), snapshot);
  await record('Preserved-volume recreation passed: IDs, password hashes, profile sentinel, image hashes and Flyway checksums unchanged.');
  await compose(['stop', 'gateway', 'backend']);
  // Only the generated schema in our generated container can be restored.
  await sql(`DROP DATABASE \`${schema}\`; CREATE DATABASE \`${schema}\`;`, { rootUser: true });
  await sql(await readFile(resolve(directory, 'backup.sql'), 'utf8'));
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240'], { timeout: 300000 });
  assert.equal(await value(snapshotQuery), snapshot);
  await record('Logical backup restored to the isolated schema; Flyway/Hibernate startup and data parity passed.');

  await compose(['stop', 'mysql']);
  await compose(['exec', '-T', 'backend', 'curl', '--fail', '--silent', 'http://127.0.0.1:8080/actuator/health/liveness']);
  const backendReadiness = await compose(['exec', '-T', 'backend', 'curl', '--silent', '--output', '/dev/null',
    '--write-out', '%{http_code}', 'http://127.0.0.1:8080/actuator/health/readiness']);
  assert.equal(backendReadiness.stdout.trim(), '503');
  await checkHttp('/actuator/health/liveness');
  // Gateway dependency probes are bounded; the backend pool has a 3-second timeout.
  await checkHttp('/actuator/health/readiness', 503);
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240'], { timeout: 300000 });
  await record('Database outage leaves gateway alive, makes readiness fail, and recovers without data reset.');

  if (browser) {
    await sql(`CREATE TABLE e2e_guard (token VARCHAR(64) NOT NULL); INSERT INTO e2e_guard VALUES ('${token}');`);
    const controlPort = await freePort(); env.E2E_CONTROL_URL = `http://127.0.0.1:${controlPort}`;
    config.services.backend.build = { context: resolve(root, 'back'), target: 'e2e' };
    config.services.backend.ports = [`127.0.0.1:${controlPort}:8080`];
    Object.assign(config.services.backend.environment, { E2E_DATABASE: schema, E2E_CONTROL_TOKEN: token });
    await saveConfig();
    await compose(['build', 'backend']);
    await compose(['up', '--detach', '--wait', '--wait-timeout', '240'], { timeout: 300000 });
    if (process.argv.includes('--verify-failure-cleanup')) throw new Error('Intentional post-startup failure to verify Compose cleanup');
    if (process.argv.includes('--verify-browser-failure')) env.E2E_FAILURE_PROBE = 'true';
    const result = await execute(process.execPath, ['node_modules/@playwright/test/cli.js', 'test',
      ...(env.E2E_FAILURE_PROBE ? ['--grep', 'register, reject bad login'] : process.argv.slice(2).filter(arg => !['--integration-only', '--verify-failure-cleanup'].includes(arg)))],
    { cwd: resolve(root, 'front'), allowFailure: true, timeout: 180000 });
    console.log(result.stdout);
    assert.equal(result.code, 0, 'Browser suite failed; see Playwright reports and Compose logs');
    await record('Browser scenarios passed through Compose gateway against isolated MySQL.');
  } else if (process.argv.includes('--verify-failure-cleanup')) {
    throw new Error('Intentional post-startup failure to verify Compose cleanup');
  }
} catch (error) {
  console.error(error.message); process.exitCode = 1;
  await writeFile(resolve(directory, 'failure.txt'), error.stack || error.message);
} finally {
  try { if (config) await cleanup(); } catch (error) { console.error(`Cleanup failed: ${error.message}`); process.exitCode = 1; }
}
