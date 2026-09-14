import { spawn } from 'node:child_process';
import { createWriteStream } from 'node:fs';
import { mkdir, readFile } from 'node:fs/promises';
import { createServer } from 'node:net';
import { delimiter, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { randomBytes } from 'node:crypto';

const front = fileURLToPath(new URL('../', import.meta.url));
const back = resolve(front, '../back');
const windows = process.platform === 'win32';
const children = new Set();
// Do not inherit developer Spring/Stripe/mail/Vite configuration or .env files.
const env = Object.fromEntries(Object.entries(process.env).filter(([key]) =>
  !/^(SPRING_|STRIPE_|APP_|SMTP_|PAYMENTS_|AUCTION_|VITE_|JAVA_TOOL_OPTIONS$|JDK_JAVA_OPTIONS$|_JAVA_OPTIONS$)/i.test(key)));
env.E2E_CONTROL_TOKEN = randomBytes(32).toString('hex');
env.VITE_API_BASE_URL = '';
env.TZ = 'UTC';
let stopping;

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
  await assertPortFree(15173);
  console.log('Compiling test-only backend launcher and building isolated frontend...');
  const mavenArgs = ['--batch-mode', '--no-transfer-progress', 'test-compile', 'dependency:build-classpath',
    '-Dmdep.outputFile=target/e2e-classpath.txt', '-Dmdep.includeScope=test'];
  if (windows) await run('cmd.exe', ['/d', '/s', '/c', 'mvnw.cmd', ...mavenArgs], back, 'build-backend.log');
  else await run('bash', ['./mvnw', ...mavenArgs], back, 'build-backend.log');
  await run(process.execPath, ['node_modules/vite/bin/vite.js', 'build', '--config', 'vite.e2e.config.js'], front, 'build-frontend.log');
  const classpath = [resolve(back, 'target/test-classes'), resolve(back, 'target/classes'),
    (await readFile(resolve(back, 'target/e2e-classpath.txt'), 'utf8')).trim()].join(delimiter);
  const java = env.JAVA_HOME ? resolve(env.JAVA_HOME, 'bin', windows ? 'java.exe' : 'java') : 'java';
  const backend = start(java, ['-Duser.timezone=UTC', '-Dspring.devtools.restart.enabled=false', '-cp', classpath,
    'e2e.E2eApplication'], back, 'backend.log');
  await ready(backend, 'http://127.0.0.1:18080/__e2e/ready', true);
  const frontend = start(process.execPath, ['node_modules/vite/bin/vite.js', 'preview', '--config', 'vite.e2e.config.js'], front, 'frontend.log');
  await ready(frontend, 'http://127.0.0.1:15173');
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
