import { spawn } from 'node:child_process';
import { createWriteStream } from 'node:fs';
import { mkdir, readFile } from 'node:fs/promises';
import { createServer } from 'node:net';
import { delimiter, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { randomBytes, webcrypto } from 'node:crypto';
import { messagePump } from './local-message-pump.mjs';

const front = fileURLToPath(new URL('../', import.meta.url));
const back = resolve(front, '../back');
const gateway = resolve(front, '../gateway');
const identity = resolve(front, '../services/identity-service');
const notification = resolve(front, '../services/notification-service');
const windows = process.platform === 'win32';
const children = new Set();
// Do not inherit developer Spring/Stripe/mail/Vite configuration or .env files.
const env = Object.fromEntries(Object.entries(process.env).filter(([key]) =>
  !/^(E2E_|IDENTITY_|NOTIFICATION_|RABBITMQ_|MYSQL_|DB_|PUBLIC_URL$|SESSION_|SPRING_|STRIPE_|APP_|SMTP_|PAYMENTS_|AUCTION_|VITE_|GATEWAY_|SERVER_|MANAGEMENT_|LOGGING_|JAVA_TOOL_OPTIONS$|JDK_JAVA_OPTIONS$|_JAVA_OPTIONS$)/i.test(key)));
env.E2E_CONTROL_TOKEN = randomBytes(32).toString('hex');
env.VITE_API_BASE_URL = '';
env.TZ = 'UTC';
let stopping;
let messages;

async function signingMaterial() {
  const kid = `e2e-${randomBytes(8).toString('hex')}`;
  const pair = await webcrypto.subtle.generateKey({
    name: 'RSASSA-PKCS1-v1_5',
    modulusLength: 2048,
    publicExponent: new Uint8Array([1, 0, 1]),
    hash: 'SHA-256',
  }, true, ['sign', 'verify']);
  const privateJwk = await webcrypto.subtle.exportKey('jwk', pair.privateKey);
  const publicJwk = await webcrypto.subtle.exportKey('jwk', pair.publicKey);
  for (const jwk of [privateJwk, publicJwk]) {
    jwk.kid = kid;
    jwk.alg = 'RS256';
    jwk.use = 'sig';
    delete jwk.key_ops;
  }
  return { signing: JSON.stringify(privateJwk), verification: JSON.stringify({ keys: [publicJwk] }) };
}

function start(command, args, cwd, log) {
  const output = log && createWriteStream(resolve(front, 'e2e-results', log));
  const child = spawn(command, args, { cwd, env, windowsHide: true, detached: !windows,
    stdio: output ? ['ignore', 'pipe', 'pipe'] : 'inherit' });
  child.done = new Promise((resolveDone) => {
    child.once('error', (error) => { console.error(error.message); resolveDone(1); });
    child.once('exit', (code) => resolveDone(code ?? 1));
  });
  children.add(child);
  if (output) {
    child.stdout.pipe(output, { end: false });
    child.stderr.pipe(output, { end: false });
    child.once('close', () => output.end());
  }
  return child;
}

async function run(command, args, cwd, log) {
  const child = start(command, args, cwd, log);
  if (await child.done !== 0) throw new Error(`${command} failed; see e2e-results/${log || ''}`);
}

async function stop() {
  if (stopping) return stopping;
  stopping = (async () => {
    if (messages) { try { await messages.stop(); } catch (error) { console.error(error.message); process.exitCode = 1; } }
    for (const child of [...children].reverse()) {
      if (!child.pid || child.exitCode !== null || child.signalCode !== null) continue;
      if (windows) {
        await new Promise((done) => spawn('taskkill', ['/pid', String(child.pid), '/T', '/F'],
          { windowsHide: true, stdio: 'ignore' }).once('exit', done));
      } else {
        try { process.kill(-child.pid, 'SIGTERM'); } catch (error) { if (error.code !== 'ESRCH') throw error; }
        await Promise.race([child.done, new Promise((done) => setTimeout(done, 3000))]);
        // Kill any remaining descendants in this owned process group, even if its leader exited.
        try { process.kill(-child.pid, 'SIGKILL'); } catch (error) { if (error.code !== 'ESRCH') throw error; }
      }
    }
  })();
  return stopping;
}

for (const [signal, code] of [['SIGINT', 130], ['SIGTERM', 143]]) {
  process.once(signal, async () => { await stop(); process.exit(code); });
}

async function assertPortFree(port) {
  await new Promise((done, reject) => {
    const server = createServer();
    server.once('error', () => reject(new Error(`Port ${port} is busy. Refusing to reuse or stop another server.`)));
    server.listen(port, '127.0.0.1', () => server.close(done));
  });
}

async function ready(child, url, control = false) {
  const deadline = Date.now() + 90000;
  while (Date.now() < deadline) {
    if (child.exitCode !== null) throw new Error(`Server exited before readiness: ${url}`);
    try {
      const response = await fetch(url, { headers: control ? { 'X-E2E-Control': env.E2E_CONTROL_TOKEN } : {},
        signal: AbortSignal.timeout(1500) });
      if (response.ok && (!control || (await response.json()).mode === 'isolated-e2e')) return;
    } catch { /* bounded readiness polling, never used for business assertions */ }
    await new Promise((done) => setTimeout(done, 250));
  }
  throw new Error(`Readiness timed out: ${url}; inspect e2e-results server logs`);
}

try {
  await mkdir(resolve(front, 'e2e-results'), { recursive: true });
  await assertPortFree(18080);
  await assertPortFree(18082);
  await assertPortFree(18083);
  await assertPortFree(15173);
  await assertPortFree(18081);
  const keys = await signingMaterial();
  env.IDENTITY_SIGNING_JWK = keys.signing;
  env.IDENTITY_VERIFICATION_JWKS = keys.verification;
  env.IDENTITY_GATEWAY_SECRET = 'gateway-e2e-secret-0000000000000000000000';
  env.IDENTITY_BACKEND_SECRET = 'backend-e2e-secret-0000000000000000000000';
  env.IDENTITY_URL = 'http://127.0.0.1:18082';
  env.PUBLIC_URL = 'http://127.0.0.1:18081';
  env.GATEWAY_IDENTITY_URL = env.IDENTITY_URL;
  env.GATEWAY_IDENTITY_SECRET = env.IDENTITY_GATEWAY_SECRET;
  env.E2E_IDENTITY_CONTROL_URL = env.IDENTITY_URL;
  env.NOTIFICATION_DELIVERY_KEY = randomBytes(32).toString('base64');
  env.NOTIFICATION_RELAY_ENABLED = 'false';
  env.E2E_CONTROL_URL = 'http://127.0.0.1:18080';
  env.E2E_NOTIFICATION_CONTROL_URL = 'http://127.0.0.1:18083';
  env.GATEWAY_NOTIFICATION_URL = env.E2E_NOTIFICATION_CONTROL_URL;
  console.log('Compiling test-only identity/backend launchers and building isolated frontend...');
  const mavenArgs = ['--batch-mode', '--no-transfer-progress', 'test-compile', 'dependency:build-classpath',
    '-Dmdep.outputFile=target/e2e-classpath.txt', '-Dmdep.includeScope=test'];
  if (windows) await run('cmd.exe', ['/d', '/s', '/c', 'mvnw.cmd', ...mavenArgs], notification, 'build-notification.log');
  else await run('bash', ['./mvnw', ...mavenArgs], notification, 'build-notification.log');
  if (windows) await run('cmd.exe', ['/d', '/s', '/c', 'mvnw.cmd', ...mavenArgs], identity, 'build-identity.log');
  else await run('bash', ['./mvnw', ...mavenArgs], identity, 'build-identity.log');
  if (windows) await run('cmd.exe', ['/d', '/s', '/c', 'mvnw.cmd', ...mavenArgs], back, 'build-backend.log');
  else await run('bash', ['./mvnw', ...mavenArgs], back, 'build-backend.log');
  const gatewayArgs = ['--batch-mode', '--no-transfer-progress', '-DskipTests', 'package'];
  if (windows) await run('cmd.exe', ['/d', '/s', '/c', 'mvnw.cmd', ...gatewayArgs], gateway, 'build-gateway.log');
  else await run('bash', ['./mvnw', ...gatewayArgs], gateway, 'build-gateway.log');
  await run(process.execPath, ['node_modules/vite/bin/vite.js', 'build', '--config', 'vite.e2e.config.js'], front, 'build-frontend.log');
  const identityClasspath = [resolve(identity, 'target/test-classes'), resolve(identity, 'target/classes'),
    (await readFile(resolve(identity, 'target/e2e-classpath.txt'), 'utf8')).trim()].join(delimiter);
  const backendClasspath = [resolve(back, 'target/test-classes'), resolve(back, 'target/classes'),
    (await readFile(resolve(back, 'target/e2e-classpath.txt'), 'utf8')).trim()].join(delimiter);
  const java = env.JAVA_HOME ? resolve(env.JAVA_HOME, 'bin', windows ? 'java.exe' : 'java') : 'java';
  const notificationClasspath = [resolve(notification, 'target/test-classes'), resolve(notification, 'target/classes'),
    (await readFile(resolve(notification, 'target/e2e-classpath.txt'), 'utf8')).trim()].join(delimiter);
  const notificationServer = start(java, ['-Duser.timezone=UTC', '-cp', notificationClasspath, 'e2e.E2eApplication'], notification, 'notification.log');
  await ready(notificationServer, 'http://127.0.0.1:18083/__e2e/ready', true);
  const identityServer = start(java, ['-Duser.timezone=UTC', '-Dspring.devtools.restart.enabled=false', '-cp', identityClasspath,
    'e2e.E2eApplication'], identity, 'identity.log');
  await ready(identityServer, 'http://127.0.0.1:18082/__e2e/ready', true);
  const backend = start(java, ['-Duser.timezone=UTC', '-Dspring.devtools.restart.enabled=false', '-cp', backendClasspath,
    'e2e.E2eApplication'], back, 'backend.log');
  await ready(backend, 'http://127.0.0.1:18080/__e2e/ready', true);
  messages = messagePump(env);
  const frontend = start(process.execPath, ['node_modules/vite/bin/vite.js', 'preview', '--config', 'vite.e2e.config.js'], front, 'frontend.log');
  await ready(frontend, 'http://127.0.0.1:15173');
  const edge = start(java, ['-jar', resolve(gateway, 'target/gateway-0.0.1-SNAPSHOT.jar'),
    '--server.address=127.0.0.1', '--server.port=18081',
    '--gateway.backend-url=http://127.0.0.1:18080', '--gateway.frontend-url=http://127.0.0.1:15173',
    '--gateway.identity-url=http://127.0.0.1:18082', '--gateway.identity-secret=' + env.IDENTITY_GATEWAY_SECRET,
    '--gateway.public-url=http://127.0.0.1:18081', '--gateway.allowed-origins=http://127.0.0.1:18081'], gateway, 'gateway.log');
  await ready(edge, 'http://127.0.0.1:18081/actuator/health/readiness');
  if (process.argv.includes('--verify-failure-cleanup')) throw new Error('Intentional failure to verify teardown');
  console.log('Isolated servers ready. Running Chromium regression suite...');
  const tests = start(process.execPath, ['node_modules/@playwright/test/cli.js', 'test', ...process.argv.slice(2)], front);
  process.exitCode = await tests.done;
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
} finally {
  await stop();
  console.log('E2E processes stopped; disposable in-memory database discarded.');
}
