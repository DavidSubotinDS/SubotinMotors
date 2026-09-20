// CI cancellation fallback. Accept only manifests created by the random-project harness.
import { readdir, readFile, writeFile } from 'node:fs/promises';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
const root = fileURLToPath(new URL('../../compose-results/', import.meta.url));
function docker(args) {
  const result = spawnSync('docker', args, { encoding: 'utf8', windowsHide: true, timeout: 60000 });
  if (result.error || result.status !== 0) throw new Error(result.error?.message || result.stderr);
  return result.stdout.trim();
}
for (const entry of await readdir(root, { withFileTypes: true }).catch(error => {
  if (error.code === 'ENOENT') return []; throw error;
})) {
  if (!entry.isDirectory() || !/^autostrada-test-[a-f0-9]{32}$/.test(entry.name)) continue;
  const manifest = JSON.parse(await readFile(resolve(root, entry.name, 'project.json'), 'utf8').catch(error => {
    if (error.code === 'ENOENT') return '{}'; throw error;
  }));
  if (manifest.project !== entry.name || manifest.schema !== `e2e_${entry.name.slice(16)}`) continue;
  for (const kind of ['container', 'network', 'volume']) {
    const query = [kind, 'ls', ...(kind === 'container' ? ['-a'] : []), '--quiet', '--filter', `label=com.docker.compose.project=${entry.name}`];
    const ids = docker(query).split(/\s+/).filter(Boolean);
    if (kind === 'container') for (const id of ids) {
      const logs = spawnSync('docker', ['logs', id], { encoding: 'utf8', windowsHide: true, timeout: 10000 });
      await writeFile(resolve(root, entry.name, `fallback-${id}.log`), (logs.stdout || '') + (logs.stderr || ''));
    }
    if (ids.length) docker([kind, 'rm', ...(kind === 'container' ? ['--force'] : []), ...ids]);
    if (docker(query)) throw new Error(`Owned ${kind} resources remain for ${entry.name}`);
  }
  for (const service of ['backend', 'frontend', 'gateway']) {
    const tag = `${entry.name}-${service}:latest`;
    const found = spawnSync('docker', ['image', 'inspect', tag], { stdio: 'ignore', windowsHide: true, timeout: 10000 });
    if (found.status === 0) docker(['image', 'rm', tag]);
  }
  console.log(`Verified cleanup: ${entry.name}`);
}
