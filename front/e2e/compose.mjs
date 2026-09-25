import { spawn } from 'node:child_process';
import { mkdir, writeFile, readFile } from 'node:fs/promises';
import { createWriteStream } from 'node:fs';
import { randomBytes, webcrypto } from 'node:crypto';
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
const identitySchema = `identity_${id}`;
const notificationSchema = `notification_${id}`;
const paymentSchema = `payment_${id}`;
const browser = !process.argv.includes('--integration-only');
const upgradeFrom = process.argv.find(arg => arg.startsWith('--upgrade-from='))?.slice('--upgrade-from='.length);
assert.ok(!upgradeFrom, 'The historical S4b --upgrade-from mode is incompatible with S6 cutover. Use the built-in staged copy/parity test.');
const env = Object.fromEntries(Object.entries(process.env).filter(([key]) =>
  !/^(COMPOSE_|MYSQL_|DB_|E2E_|IDENTITY_|NOTIFICATION_|PAYMENT_|RABBITMQ_|SPRING_|STRIPE_|APP_|SMTP_|PAYMENTS_|AUCTION_|VITE_|GATEWAY_|PUBLIC_URL$|SESSION_|SERVER_|MANAGEMENT_|LOGGING_|JAVA_TOOL_OPTIONS$|JDK_JAVA_OPTIONS$|_JAVA_OPTIONS$)/i.test(key)));
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

async function signingMaterial() {
  const kid = `compose-${randomBytes(8).toString('hex')}`;
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
async function eventually(check, description) {
  const deadline = Date.now() + 45000;
  while (Date.now() < deadline) { if (await check()) return; await new Promise(done => setTimeout(done, 250)); }
  throw new Error(`Timed out: ${description}`);
}
async function sql(statement, { rootUser = false, allowFailure = false } = {}) {
  return compose(['exec', '-T', 'mysql', 'sh', '-c', rootUser
    ? 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --skip-column-names "$MYSQL_DATABASE"'
    : 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u"$MYSQL_USER" --batch --skip-column-names "$MYSQL_DATABASE"'],
  { input: statement, allowFailure, log: false, timeout: 20000 });
}
async function runtimeSql(statement, { allowFailure = false } = {}) {
  return compose(['exec', '-T', '-e', `DB_RUNTIME_USERNAME=${env.DB_RUNTIME_USERNAME}`,
    '-e', `DB_RUNTIME_PASSWORD=${env.DB_RUNTIME_PASSWORD}`, 'mysql', 'sh', '-c',
    'MYSQL_PWD="$DB_RUNTIME_PASSWORD" exec mysql -u"$DB_RUNTIME_USERNAME" --batch --skip-column-names "$MYSQL_DATABASE"'],
  { input: statement, allowFailure, log: false, timeout: 20000 });
}
async function value(statement) { return (await sql(statement)).stdout.trim(); }
async function rootValue(statement) { return (await sql(statement, { rootUser: true })).stdout.trim(); }
async function checkHttp(path, status = 200, options = {}) {
  const response = await fetch(env.PUBLIC_URL + path, { redirect: 'manual', signal: AbortSignal.timeout(10000), ...options });
  assert.equal(response.status, status, `${path}: ${await response.clone().text()}`);
  assert.match(response.headers.get('x-request-id') || '', /^[a-f0-9-]{36}$/);
  return response;
}
async function record(message) { evidence.push(message); console.log(message); await writeFile(resolve(directory, 'evidence.json'), JSON.stringify({ project, evidence }, null, 2)); }
function mysqlLiteral(value) { return `'${String(value).replaceAll("'", "''")}'`; }
async function captureFailureDiagnostics(error) {
  await writeFile(resolve(directory, 'failure.txt'), error?.stack || String(error));
  const captures = [
    ['failure-compose-ps.log', ['ps', '--all']],
    ['failure-mysql.log', ['logs', '--no-color', 'mysql']],
    ['failure-compose.log', ['logs', '--no-color']],
  ];
  for (const [file, args] of captures) {
    try {
      const result = await compose(args, { allowFailure: true, log: false, timeout: 60000 });
      const output = `${result.stdout}${result.stderr}`;
      await writeFile(resolve(directory, file), output);
      if (file !== 'failure-compose-ps.log') console.error(`\n--- ${file} ---\n${output}`);
    } catch (captureError) {
      await writeFile(resolve(directory, file), `Unable to collect diagnostics:\n${captureError?.stack || captureError}`);
    }
  }
}
async function copyIdentityTables() {
  const tables = ['tb_password_reset_token', 'tb_profile_picture', 'tb_user_profile', 'tb_role', 'tb_user'];
  config.services['identity-copy'] = {
    build: { context: resolve(root, 'services/identity-service'), target: 'e2e' },
    entrypoint: ['java', '-cp', '/test/test-classes:/test/classes:/test/lib/*'],
    command: ['lithan.autostrada.identity.migration.IdentityCopy'],
    environment: {
      CUTOVER_WRITE_FREEZE: 'I_HAVE_STOPPED_ALL_WRITERS',
      CUTOVER_SOURCE_TIMEZONE: 'UTC',
      CUTOVER_SOURCE_URL: `jdbc:mysql://mysql:3306/${schema}?serverTimezone=UTC&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&allowPublicKeyRetrieval=true&useSSL=false`,
      CUTOVER_SOURCE_USER: 'e2e',
      CUTOVER_SOURCE_PASSWORD: env.MYSQL_PASSWORD,
      CUTOVER_TARGET_URL: env.IDENTITY_DB_URL,
      CUTOVER_TARGET_USER: env.IDENTITY_DB_USERNAME,
      CUTOVER_TARGET_PASSWORD: env.IDENTITY_DB_PASSWORD,
    },
    networks: ['edge', 'database'],
  };
  await saveConfig();
  await compose(['build', 'identity-copy'], { timeout: 300000 });
  await compose(['run', '--rm', '--no-deps', 'identity-copy'], { timeout: 120000 });
  delete config.services['identity-copy'];
  await saveConfig();
  const source = await rootValue(tables.map(table => `SELECT '${table}',COUNT(*) FROM \`${schema}\`.\`${table}\``).join(';'));
  const target = await rootValue(tables.map(table => `SELECT '${table}',COUNT(*) FROM \`${identitySchema}\`.\`${table}\``).join(';'));
  assert.equal(target, source, 'Identity copy count parity failed after production utility');
  await record('S5 cutover copy passed: the production IdentityCopy utility copied five identity-owned tables and verified complete row-value parity before writing the backend cutover marker.');
}
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
  const keys = await signingMaterial();
  Object.assign(env, {
    IDENTITY_DB_URL: `jdbc:mysql://mysql:3306/${identitySchema}?serverTimezone=UTC&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&allowPublicKeyRetrieval=true&useSSL=false`,
    IDENTITY_DB_NAME: identitySchema,
    NOTIFICATION_DB_NAME: notificationSchema,
    NOTIFICATION_DB_USERNAME: 'notification_e2e',
    NOTIFICATION_DB_PASSWORD: randomBytes(32).toString('hex'),
    PAYMENT_DB_NAME: paymentSchema,
    PAYMENT_DB_USERNAME: 'payment_e2e',
    PAYMENT_DB_PASSWORD: randomBytes(32).toString('hex'),
    NOTIFICATION_DELIVERY_KEY: randomBytes(32).toString('base64'),
    RABBITMQ_BACKEND_PASSWORD: randomBytes(32).toString('hex'),
    RABBITMQ_IDENTITY_PASSWORD: randomBytes(32).toString('hex'),
    RABBITMQ_NOTIFICATION_PASSWORD: randomBytes(32).toString('hex'),
    RABBITMQ_PAYMENT_PASSWORD: randomBytes(32).toString('hex'),
    RABBITMQ_OPERATOR_PASSWORD: randomBytes(32).toString('hex'),
    IDENTITY_DB_USERNAME: 'identity_e2e',
    IDENTITY_DB_PASSWORD: randomBytes(32).toString('hex'),
    DB_RUNTIME_USERNAME: 'backend_runtime',
    DB_RUNTIME_PASSWORD: randomBytes(32).toString('hex'),
    DB_MIGRATION_USERNAME: 'backend_migration',
    DB_MIGRATION_PASSWORD: randomBytes(32).toString('hex'),
    IDENTITY_DATABASE: identitySchema,
    IDENTITY_SIGNING_JWK: keys.signing,
    IDENTITY_VERIFICATION_JWKS: keys.verification,
    IDENTITY_GATEWAY_SECRET: 'gateway-compose-secret-000000000000000000',
    IDENTITY_BACKEND_SECRET: 'backend-compose-secret-000000000000000000',
  });
  config = { services: {
    mysql: { environment: { AUTOSTRADA_E2E: 'true' } },
    backend: { environment: { AUCTION_NOTIFICATION_SCHEDULING: 'false',
      SPRING_FLYWAY_TARGET: '18',
      // S7 entities are present in the new binary before V22 can run. Bootstrap
      // stages deliberately stop at V18/V20 while copy cutovers are prepared.
      SPRING_JPA_HIBERNATE_DDL_AUTO: 'none' } },
  } };
  if (upgradeFrom) {
    assert.ok(!browser, '--upgrade-from requires --integration-only');
    config.services.backend.build = { context: resolve(root, upgradeFrom), target: 'production' };
  }
  await saveConfig();
  // Refuse a collision before creating anything, even though the name has 128 random bits.
  for (const kind of ['container', 'volume', 'network']) {
    const result = await execute('docker', [kind, 'ls', ...(kind === 'container' ? ['-a'] : []), '--quiet',
      '--filter', `label=com.docker.compose.project=${project}`]);
    assert.equal(result.stdout.trim(), '', 'Project collision; refusing reuse');
  }
  owned = true;
  await writeFile(resolve(directory, 'project.json'), JSON.stringify({ project, port, schema, identitySchema, paymentSchema }));
  await compose(['config', '--quiet']);
  // Reproduce Linux's sourced (mode 100644) init hook even on Windows mounts.
  // Uses the actual pinned image's entrypoint helpers, with SQL stubbed.
  await compose(['run', '--rm', '--no-deps', '--entrypoint', 'bash', '--volume',
    `${resolve(root, 'back/scripts')}:/checks:ro`, 'mysql', '/checks/test-mysql-init.sh']);
  await record('MySQL init-hook regression passed for sourced/executable scripts and E2E skip; parent shell options preserved.');
  console.log(`Building production images for ${project}; logs: ${directory}`);
  await compose(['build']);
  const seedGuard = await compose(['run', '--rm', '--no-deps', '-e', 'APP_DEMO_DATA_ACK=', 'backend'], { allowFailure: true });
  assert.equal(seedGuard.code, 1, 'Production startup must refuse missing demo acknowledgement');
  assert.match(seedGuard.stderr + seedGuard.stdout, /I_ACCEPT_EXISTING_DEMO_DATA/);
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240', 'mysql'], { timeout: 300000 });
  await sql(`CREATE DATABASE IF NOT EXISTS \`${identitySchema}\`;
    CREATE DATABASE IF NOT EXISTS \`${notificationSchema}\`;
    CREATE DATABASE IF NOT EXISTS \`${paymentSchema}\`;
    CREATE USER IF NOT EXISTS 'notification_e2e'@'%' IDENTIFIED BY ${mysqlLiteral(env.NOTIFICATION_DB_PASSWORD)};
    GRANT ALL PRIVILEGES ON \`${notificationSchema}\`.* TO 'notification_e2e'@'%';
    CREATE USER IF NOT EXISTS 'payment_e2e'@'%' IDENTIFIED BY ${mysqlLiteral(env.PAYMENT_DB_PASSWORD)};
    GRANT ALL PRIVILEGES ON \`${paymentSchema}\`.* TO 'payment_e2e'@'%';
    CREATE USER IF NOT EXISTS 'identity_e2e'@'%' IDENTIFIED BY ${mysqlLiteral(env.IDENTITY_DB_PASSWORD)};
    GRANT ALL PRIVILEGES ON \`${identitySchema}\`.* TO 'identity_e2e'@'%';
    CREATE USER IF NOT EXISTS 'backend_runtime'@'%' IDENTIFIED BY ${mysqlLiteral(env.DB_RUNTIME_PASSWORD)};
    CREATE USER IF NOT EXISTS 'backend_migration'@'%' IDENTIFIED BY ${mysqlLiteral(env.DB_MIGRATION_PASSWORD)};
    GRANT ALL PRIVILEGES ON \`${schema}\`.* TO 'backend_runtime'@'%';
    GRANT ALL PRIVILEGES ON \`${schema}\`.* TO 'backend_migration'@'%' WITH GRANT OPTION;`, { rootUser: true });
  Object.assign(config.services.backend.environment, {
    DB_USERNAME: env.DB_RUNTIME_USERNAME,
    DB_PASSWORD: env.DB_RUNTIME_PASSWORD,
    DB_RUNTIME_USERNAME: env.DB_RUNTIME_USERNAME,
    SPRING_FLYWAY_USER: env.DB_MIGRATION_USERNAME,
    SPRING_FLYWAY_PASSWORD: env.DB_MIGRATION_PASSWORD,
  });
  await saveConfig();
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240', 'identity'], { timeout: 300000 });
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240', 'backend'], { timeout: 300000 });
  assert.equal(await value('SELECT COUNT(*) FROM flyway_schema_history WHERE success=1 AND version BETWEEN 1 AND 18'), '18');
  await copyIdentityTables();
  config.services.backend.environment.SPRING_FLYWAY_TARGET = '20';
  await saveConfig();
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240', 'backend', 'notification', 'rabbitmq'], { timeout: 300000 });
  await compose(['stop', 'backend', 'notification']);
  // Existing-data cutover: preserve both read and unread legacy inbox entries.
  await sql(`INSERT INTO tb_auction_notification(id_user,id_car,notification_type,message,created_at,read_at)
    SELECT 2,id_car,'AUCTION_ENDING_SOON',CONCAT('S6 import ',id_car),'2025-01-01 12:00:00.123456',
      CASE WHEN id_car=1 THEN '2025-01-01 12:01:00.123456' ELSE NULL END
    FROM tb_car WHERE id_car IN (1,2)`);
  assert.equal(await value("SELECT COUNT(*) FROM tb_auction_notification WHERE message LIKE 'S6 import %'"), '2');
  const importedCount = await value('SELECT COUNT(*) FROM tb_auction_notification');
  const importedReadCount = await value('SELECT COUNT(*) FROM tb_auction_notification WHERE read_at IS NOT NULL');
  config.services['notification-copy'] = {
    build: { context: resolve(root, 'services/notification-service'), target: 'e2e' },
    entrypoint: ['java', '-cp', '/test/test-classes:/test/classes:/test/lib/*'],
    command: ['lithan.autostrada.notification.NotificationCopy'],
    environment: {
      CUTOVER_WRITE_FREEZE: 'I_HAVE_STOPPED_ALL_WRITERS', CUTOVER_SOURCE_TIMEZONE: 'UTC',
      CUTOVER_SOURCE_URL: `jdbc:mysql://mysql:3306/${schema}?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false`,
      CUTOVER_SOURCE_USER: 'e2e', CUTOVER_SOURCE_PASSWORD: env.MYSQL_PASSWORD,
      CUTOVER_TARGET_URL: `jdbc:mysql://mysql:3306/${notificationSchema}?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false`,
      CUTOVER_TARGET_USER: env.NOTIFICATION_DB_USERNAME, CUTOVER_TARGET_PASSWORD: env.NOTIFICATION_DB_PASSWORD,
    }, networks: ['database'],
  };
  await saveConfig();
  await compose(['build', 'notification-copy']);
  await compose(['run', '--rm', '--no-deps', 'notification-copy']);
  assert.equal(await rootValue(`SELECT COUNT(*) FROM \`${notificationSchema}\`.tb_notification`), importedCount);
  assert.equal(await rootValue(`SELECT COUNT(*) FROM \`${notificationSchema}\`.tb_notification WHERE read_at IS NOT NULL`), importedReadCount);
  delete config.services['notification-copy'];
  delete config.services.backend.environment.SPRING_FLYWAY_TARGET;
  delete config.services.backend.environment.SPRING_JPA_HIBERNATE_DDL_AUTO;
  await saveConfig();
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240', 'backend', 'payment', 'notification', 'rabbitmq'], { timeout: 300000 });
  await compose(['stop', 'backend', 'payment']);
  const importedAttempt = '810e61d2-8892-4b17-a338-a91c8ef50301';
  await sql(`INSERT INTO tb_checkout_attempt(attempt_id,id_user,purpose,client_request_id,request_hash,customer_email,status,provider_session_id,provider_checkout_url,payment_intent_id,failure_count,expires_at,created_at,updated_at,version)
    VALUES ('${importedAttempt}',3,'STORE_ORDER','s8-existing','aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa','buyer@example.test','CHECKOUT_CREATED','cs_s8_existing','https://checkout.stripe.test/existing','pi_s8_existing',0,CURRENT_TIMESTAMP + INTERVAL 1 DAY,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,2);
    INSERT INTO tb_store_order(id_user,total_minor,currency,status,shipping_name,shipping_address,shipping_street_address,shipping_city,shipping_postal_code,shipping_country,checkout_session_id,checkout_url,payment_intent_id,created_at,updated_at,version,checkout_attempt_id)
    VALUES (3,19900,'eur','CHECKOUT_CREATED','S8 Buyer','Test Street 1','Test Street 1','Test City','10000','RS','cs_s8_existing','https://checkout.stripe.test/existing','pi_s8_existing',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,'${importedAttempt}');
    UPDATE tb_checkout_attempt SET aggregate_id=(SELECT id_order FROM tb_store_order WHERE checkout_attempt_id='${importedAttempt}') WHERE attempt_id='${importedAttempt}';
    INSERT INTO tb_checkout_webhook_inbox(provider_event_id,event_type,checkout_session_id,payment_intent_id,payment_status,status,delivery_count,received_at,updated_at,processed_at)
    VALUES ('evt_s8_existing','checkout.session.completed','cs_s8_existing','pi_s8_existing','paid','PROCESSED',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
    UPDATE payment_cutover SET state='COPYING',verified_at=NULL WHERE id=1;`);
  config.services['payment-copy'] = {
    build: { context: resolve(root, 'services/payment-service'), target: 'e2e' },
    entrypoint: ['java', '-cp', '/test/test-classes:/test/classes:/test/lib/*'],
    command: ['lithan.autostrada.payment.PaymentCopy'],
    environment: {
      CUTOVER_WRITE_FREEZE: 'I_HAVE_STOPPED_ALL_PAYMENT_WRITERS', CUTOVER_SOURCE_TIMEZONE: 'UTC',
      CUTOVER_SOURCE_URL: `jdbc:mysql://mysql:3306/${schema}?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false`,
      CUTOVER_SOURCE_USER: 'e2e', CUTOVER_SOURCE_PASSWORD: env.MYSQL_PASSWORD,
      CUTOVER_TARGET_URL: `jdbc:mysql://mysql:3306/${paymentSchema}?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false`,
      CUTOVER_TARGET_USER: env.PAYMENT_DB_USERNAME, CUTOVER_TARGET_PASSWORD: env.PAYMENT_DB_PASSWORD,
    }, networks: ['database'],
  };
  await saveConfig();
  await compose(['build', 'payment-copy']);
  await compose(['run', '--rm', '--no-deps', 'payment-copy'], { timeout: 120000 });
  assert.equal(await rootValue(`SELECT COUNT(*) FROM \`${paymentSchema}\`.payment_attempt`), await value('SELECT COUNT(*) FROM tb_checkout_attempt'));
  assert.equal(await rootValue(`SELECT SUM(amount_minor) FROM \`${paymentSchema}\`.payment_attempt`), await value('SELECT SUM(CASE WHEN purpose=\'STORE_ORDER\' THEN o.total_minor ELSE d.amount_minor END) FROM tb_checkout_attempt a LEFT JOIN tb_store_order o ON o.checkout_attempt_id=a.attempt_id LEFT JOIN tb_listing_deposit d ON d.checkout_attempt_id=a.attempt_id'));
  assert.equal(await rootValue(`SELECT CONCAT(provider_session_id,'|',provider_payment_intent_id) FROM \`${paymentSchema}\`.payment_attempt WHERE attempt_id='${importedAttempt}'`), 'cs_s8_existing|pi_s8_existing');
  assert.equal(await value('SELECT state FROM payment_cutover WHERE id=1'), 'PARITY_VERIFIED');
  assert.equal(await rootValue(`SELECT state FROM \`${paymentSchema}\`.payment_copy_checkpoint WHERE id=1`), 'PARITY_VERIFIED');
  delete config.services['payment-copy']; await saveConfig();
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240'], { timeout: 300000 });
  if (upgradeFrom) {
    // Start the old production binary first, then replace only the backend on
    // the same isolated MySQL volume. Never import a developer database.
    await sql("UPDATE tb_user_profile SET about='S4b upgrade sentinel' WHERE id_profile=1");
    const tables = (await value('SHOW TABLES')).split(/\r?\n/);
    assert.ok(tables.length > 0 && tables.every(table => /^[a-zA-Z0-9_]+$/.test(table)));
    const checksumQuery = `CHECKSUM TABLE ${tables.map(table => `\`${table}\``).join(',')}`;
    const beforeUpgrade = await value(checksumQuery);
    assert.ok(!beforeUpgrade.includes('NULL'), 'Every table must support checksum comparison');
    await compose(['stop', 'gateway', 'backend']);
    config.services.backend.build = { context: resolve(root, 'back'), target: 'production' };
    await saveConfig();
    await compose(['build', 'backend']);
    await compose(['up', '--detach', '--wait', '--wait-timeout', '240'], { timeout: 300000 });
    assert.equal(await value(checksumQuery), beforeUpgrade, 'Old-runtime data changed during S4b startup');
    await record('Old production backend to current production backend upgrade passed on the same MySQL volume: every table checksum preserved.');
  }
  await compose(['images', '--format', 'json']);
  assert.equal(await value('SELECT COUNT(*) FROM flyway_schema_history WHERE success=1 AND version BETWEEN 1 AND 19'), '19');
  assert.equal(await rootValue(`SELECT COUNT(*) FROM \`${identitySchema}\`.flyway_schema_history WHERE success=1`), '2');
  assert.equal(await value('SELECT COUNT(*) FROM flyway_schema_history WHERE success=1'), '26');
  assert.equal(await value("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('tb_checkout_attempt','tb_stock_hold','tb_checkout_webhook_inbox')"), '3');
  assert.equal(await value("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND column_name='checkout_attempt_id' AND table_name IN ('tb_store_order','tb_listing_deposit')"), '2');
  assert.equal(await rootValue(`SELECT COUNT(*) FROM \`${notificationSchema}\`.flyway_schema_history WHERE success=1`), '1');
  assert.equal(await rootValue(`SELECT COUNT(*) FROM \`${paymentSchema}\`.flyway_schema_history WHERE success=1`), '1');
  await record('S8 payment copy passed with attempt count, amount and provider-ID parity; source and target cutover markers are PARITY_VERIFIED.');
  assert.notEqual((await runtimeSql('SELECT COUNT(*) FROM archive_notification_tb_auction_notification', { allowFailure: true })).code, 0);
  await record('S6 notification copy parity, V21 archive cutover and runtime archive denial passed.');
  config.services['broker-probe'] = {
    build: { context: resolve(root, 'services/notification-service'), target: 'e2e' },
    entrypoint: ['java', '-cp', '/test/test-classes:/test/classes:/test/lib/*'],
    command: ['lithan.autostrada.notification.BrokerProbe'],
    environment: {
      E2E_NOTIFICATION_DATABASE: notificationSchema, NOTIFICATION_DB_USERNAME: env.NOTIFICATION_DB_USERNAME,
      NOTIFICATION_DB_PASSWORD: env.NOTIFICATION_DB_PASSWORD, RABBITMQ_BACKEND_PASSWORD: env.RABBITMQ_BACKEND_PASSWORD,
      RABBITMQ_IDENTITY_PASSWORD: env.RABBITMQ_IDENTITY_PASSWORD, RABBITMQ_OPERATOR_PASSWORD: env.RABBITMQ_OPERATOR_PASSWORD,
    }, networks: ['database', 'edge'],
  };
  await saveConfig();
  await compose(['build', 'broker-probe']);
  await compose(['run', '--rm', '--no-deps', 'broker-probe'], { timeout: 120000 });
  delete config.services['broker-probe']; await saveConfig();
  await record('Real RabbitMQ/MySQL delivery, event/business dedupe, producer permissions, poison quarantine, repaired redrive and delayed retry passed.');
  assert.equal(await value('SELECT COUNT(*) FROM flyway_schema_history WHERE success=0'), '0');
  assert.equal((await runtimeSql('SELECT COUNT(*) FROM tb_car_part')).code, 0, 'Backend runtime lost business-table access');
  assert.notEqual((await runtimeSql('SELECT COUNT(*) FROM archive_identity_tb_user', { allowFailure: true })).code, 0,
    'Backend runtime still has access to archived identity data');
  await record('S5 credential isolation passed: backend runtime retains business-table access but cannot read archived identity tables after V19.');
  await writeFile(resolve(directory, 'migrations.tsv'), (await sql('SELECT version,script,checksum,success FROM flyway_schema_history ORDER BY installed_rank')).stdout);
  await writeFile(resolve(directory, 'identity-migrations.tsv'),
    (await sql(`SELECT version,script,checksum,success FROM \`${identitySchema}\`.flyway_schema_history ORDER BY installed_rank`, { rootUser: true })).stdout);
  for (const service of ['identity', 'notification', 'payment', 'backend', 'frontend', 'gateway']) {
    assert.notEqual((await compose(['exec', '-T', service, 'id', '-u'])).stdout.trim(), '0');
  }
  const rendered = JSON.parse((await compose(['config', '--format', 'json'], { log: false })).stdout);
  for (const service of ['mysql', 'rabbitmq', 'notification', 'identity', 'payment', 'backend', 'frontend']) assert.ok(!rendered.services[service].ports?.length, `${service} unexpectedly published`);
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
  assert.match(cookie, /AUTOSTRADA_SESSION=/); assert.match(cookie, /HttpOnly/i); assert.match(cookie, /SameSite=Lax/i);
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
  await compose(['stop', 'rabbitmq']);
  await checkHttp('/api/user/notifications', 200, { headers: { Cookie: signedInCookie } });
  const probeCar = await value("INSERT INTO tb_car(make,model,production_year,status,price,id_user,auction_end_time) VALUES ('S6','Outage Probe','2025','ACTIVE',10000,1,CURRENT_TIMESTAMP + INTERVAL 2 HOUR); SELECT LAST_INSERT_ID()");
  await checkHttp(`/api/user/auctions/${probeCar}/follow`, 200, { method: 'POST', headers: { Cookie: signedInCookie, 'X-CSRF-TOKEN': signedInToken } });
  assert.equal(await value(`SELECT COUNT(*) FROM tb_notification_outbox WHERE dedupe_key LIKE '%:${probeCar}:ENDING_SOON' AND published_at IS NULL`), '1');
  await checkHttp('/api/auth/password-reset', 200, { method: 'POST', headers: { 'Content-Type': 'application/json',
    Cookie: signedInCookie, 'X-CSRF-TOKEN': signedInToken },
    body: JSON.stringify({ identifier: 'demo_bidder' }) });
  const resetLogs = (await compose(['logs', '--no-color', 'identity'], { log: false })).stdout;
  assert.ok(!/reset-password\?token=/.test(resetLogs), 'Reset links must not enter application logs');
  assert.equal(await rootValue(`SELECT COUNT(*) FROM \`${identitySchema}\`.tb_password_reset_token`), '1');
  assert.equal(await rootValue(`SELECT COUNT(*) FROM \`${identitySchema}\`.tb_delivery_outbox WHERE published_at IS NULL AND encrypted_payload IS NOT NULL`), '1');
  await compose(['up', '--detach', '--wait', '--wait-timeout', '120', 'rabbitmq']);
  await eventually(async () => await value(`SELECT COUNT(*) FROM tb_notification_outbox WHERE dedupe_key LIKE '%:${probeCar}:ENDING_SOON' AND published_at IS NOT NULL`) === '1', 'backend outbox recovery');
  await eventually(async () => await rootValue(`SELECT COUNT(*) FROM \`${notificationSchema}\`.tb_notification WHERE id_car=${probeCar}`) === '1', 'notification recovery');
  await eventually(async () => await rootValue(`SELECT COUNT(*) FROM \`${notificationSchema}\`.tb_delivery WHERE state='SUPPRESSED' AND encrypted_payload IS NULL`) === '1', 'encrypted reset delivery recovery');
  await record('Broker outage preserves ending-soon and encrypted reset intents; inbox reads remain available and both outboxes recover after broker restart. Production log-mode delivery is suppressed, not real SMTP.');
  await checkHttp('/api/auth/logout', 200, { method: 'POST', headers: { Cookie: signedInCookie, 'X-CSRF-TOKEN': signedInToken } });
  await record('Production images started non-root; private service ports, SPA/API/exact webhook routing and backend/identity MySQL migrations verified.');
  await record('Production cookie attributes, login rotation/old-session rejection, CSRF, logout, CORS, trusted redirects and reset request without secret logging verified.');

  // Real MySQL checks: grants, unique/CHECK enforcement, S5 identity FK extraction, and a competing row lock.
  assert.notEqual((await sql('SELECT * FROM mysql.user', { allowFailure: true })).code, 0);
  for (const query of [
    `INSERT INTO \`${identitySchema}\`.tb_role(role,id_user) VALUES ('ROLE_USER',999999)`,
    `INSERT INTO \`${identitySchema}\`.tb_user(username,password,email) SELECT username,password,email FROM \`${identitySchema}\`.tb_user LIMIT 1`,
    'UPDATE tb_car_part SET stock_quantity=-1 WHERE id_part=1']) {
    assert.notEqual((await sql(query, { rootUser: query.includes(identitySchema), allowFailure: true })).code, 0, 'MySQL constraint did not reject invalid data');
  }
  // S5 drops cross-boundary FKs after the copy/cutover marker but preserves
  // scalar owner columns, nullability and indexes for query plans and checks.
  const identityReferences = [
    ['tb_car', 'id_car', 'id_user'], ['tb_car_bid', 'id_bid', 'id_user'],
    ['tb_test_drive', 'id_test_drive', 'id_user'], ['tb_car_listing', 'id_listing', 'id_seller'],
    ['tb_listing_test_ride', 'id_test_ride', 'id_user'], ['tb_listing_deposit', 'id_deposit', 'id_buyer'],
    ['tb_auction_follow', 'id_follow', 'id_user'], ['archive_notification_tb_auction_notification', 'id_notification', 'id_user'],
    ['tb_listing_comment', 'id_comment', 'id_user'], ['tb_cart_item', 'id_cart_item', 'id_user'],
    ['tb_store_order', 'id_order', 'id_user'], ['tb_payment_account', 'id_payment_account', 'id_user'],
    ['tb_payment_order', 'id_payment', 'id_buyer'], ['tb_payment_order', 'id_payment', 'id_seller'],
  ];
  for (const [table, , column] of identityReferences) {
    assert.equal(await value(`SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='${table}' AND COLUMN_NAME='${column}' AND REFERENCED_TABLE_NAME='tb_user' AND REFERENCED_COLUMN_NAME='id_user'`), '0');
    assert.equal(await value(`SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='${table}' AND COLUMN_NAME='${column}'`), 'NO');
    assert.ok(Number(await value(`SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='${table}' AND COLUMN_NAME='${column}' AND SEQ_IN_INDEX=1`)) > 0);
    if (Number(await value(`SELECT COUNT(*) FROM ${table}`)) > 0) {
      const orphan = await sql(`START TRANSACTION; UPDATE ${table} SET ${column}=2147483647 LIMIT 1; ROLLBACK;`);
      assert.equal(orphan.code, 0);
    }
  }
  assert.equal(await value("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='archive_identity_tb_user'"), '1');
  const identitySnapshotQuery = identityReferences.map(([table, key, column]) =>
    `SELECT '${table}.${column}',${key},${column} FROM ${table} ORDER BY ${key}`).join(';');
  const identitySnapshot = await value(identitySnapshotQuery);
  await record('S5: all 14 scalar identity columns remain non-null and indexed after backend V19; database FKs to identity tables are removed only after copy parity.');

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
  await record('MySQL schema-only grants, identity unique/FK constraints, business CHECK constraints and row-lock contention verified.');

  await sql(`UPDATE \`${identitySchema}\`.tb_user_profile SET about='S5 backup sentinel' WHERE id_profile=1`, { rootUser: true });
  const snapshotQuery = "SELECT version,checksum FROM flyway_schema_history ORDER BY installed_rank; SELECT id_picture,SHA2(image,256) FROM tb_car_gallery_picture ORDER BY id_picture";
  const snapshot = await value(snapshotQuery);
  const notificationSnapshotQuery = `SELECT id_notification,id_user,id_car,notification_type,message,created_at,read_at,SHA2(auction_snapshot,256),dedupe_key FROM \`${notificationSchema}\`.tb_notification ORDER BY id_notification;
    SELECT consumer_name,event_id,received_at FROM \`${notificationSchema}\`.tb_message_inbox ORDER BY consumer_name,event_id;
    SELECT delivery_id,state,expires_at,encrypted_payload IS NULL FROM \`${notificationSchema}\`.tb_delivery ORDER BY delivery_id;
    SELECT version,checksum FROM \`${notificationSchema}\`.flyway_schema_history ORDER BY installed_rank`;
  const notificationSnapshot = await rootValue(notificationSnapshotQuery);
  const paymentSnapshotQuery = `SELECT payment_id,attempt_id,source_service,business_type,business_id,buyer_id,amount_minor,currency,status,provider_session_id,provider_payment_intent_id,aggregate_version FROM \`${paymentSchema}\`.payment_attempt ORDER BY payment_id;
    SELECT provider_event_id,event_type,provider_session_id,provider_payment_intent_id,status,delivery_count,payload_hash FROM \`${paymentSchema}\`.payment_webhook_receipt ORDER BY provider_event_id;
    SELECT original_id,user_id,provider_account_id,status,transfers_enabled FROM \`${paymentSchema}\`.payment_provider_account_audit ORDER BY original_id;
    SELECT original_id,bid_id,buyer_id,seller_id,amount_minor,platform_fee_minor,currency,status,purpose,provider_session_id,provider_payment_intent_id,version FROM \`${paymentSchema}\`.payment_legacy_audit ORDER BY original_id;
    SELECT version,checksum FROM \`${paymentSchema}\`.flyway_schema_history ORDER BY installed_rank`;
  const paymentSnapshot = await rootValue(paymentSnapshotQuery);
  const identityDataSnapshot = await rootValue(`SELECT id_user,username,email,password FROM \`${identitySchema}\`.tb_user ORDER BY id_user;
    SELECT id_profile,about FROM \`${identitySchema}\`.tb_user_profile ORDER BY id_profile;
    SELECT id_token,SHA2(token_hash,256),consumed_at FROM \`${identitySchema}\`.tb_password_reset_token ORDER BY id_token;
    SELECT version,checksum FROM \`${identitySchema}\`.flyway_schema_history ORDER BY installed_rank`);
  const dump = await compose(['exec', '-T', 'mysql', 'sh', '-c',
    `MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --no-tablespaces --set-gtid-purged=OFF --hex-blob --databases "$MYSQL_DATABASE" "${identitySchema}" "${notificationSchema}" "${paymentSchema}"`], { log: false });
  await writeFile(resolve(directory, 'backup.sql'), dump.stdout);
  await compose(['down', '--timeout', '20']); // Intentionally preserve volume.
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240'], { timeout: 300000 });
  assert.equal(await value(snapshotQuery), snapshot);
  assert.equal(await value(identitySnapshotQuery), identitySnapshot);
  assert.equal(await rootValue(`SELECT id_user,username,email,password FROM \`${identitySchema}\`.tb_user ORDER BY id_user;
    SELECT id_profile,about FROM \`${identitySchema}\`.tb_user_profile ORDER BY id_profile;
    SELECT id_token,SHA2(token_hash,256),consumed_at FROM \`${identitySchema}\`.tb_password_reset_token ORDER BY id_token;
    SELECT version,checksum FROM \`${identitySchema}\`.flyway_schema_history ORDER BY installed_rank`), identityDataSnapshot);
  await record('Preserved-volume recreation passed: business IDs, identity IDs, password hashes, reset state, profile sentinel, image hashes and Flyway checksums unchanged.');
  assert.equal(await rootValue(notificationSnapshotQuery), notificationSnapshot);
  assert.equal(await rootValue(paymentSnapshotQuery), paymentSnapshot);
  await record('S8: payment attempts, provider identifiers, receipts, audit rows and migration checksums survived volume restart.');
  await compose(['stop', 'gateway', 'backend', 'identity', 'notification', 'payment']);
  // Only the generated schemas in our generated container can be restored.
  await sql(`DROP DATABASE \`${schema}\`; DROP DATABASE \`${identitySchema}\`; DROP DATABASE \`${notificationSchema}\`; DROP DATABASE \`${paymentSchema}\`;`, { rootUser: true });
  await compose(['exec', '-T', 'mysql', 'sh', '-c', 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot'],
    { input: await readFile(resolve(directory, 'backup.sql'), 'utf8'), log: false, timeout: 90000 });
  await compose(['up', '--detach', '--wait', '--wait-timeout', '240'], { timeout: 300000 });
  assert.equal(await value(snapshotQuery), snapshot);
  assert.equal(await value(identitySnapshotQuery), identitySnapshot);
  assert.equal(await rootValue(`SELECT id_user,username,email,password FROM \`${identitySchema}\`.tb_user ORDER BY id_user;
    SELECT id_profile,about FROM \`${identitySchema}\`.tb_user_profile ORDER BY id_profile;
    SELECT id_token,SHA2(token_hash,256),consumed_at FROM \`${identitySchema}\`.tb_password_reset_token ORDER BY id_token;
    SELECT version,checksum FROM \`${identitySchema}\`.flyway_schema_history ORDER BY installed_rank`), identityDataSnapshot);
  await record('S5: all business row IDs, scalar identity references, identity credentials, profile data and reset state survived restart and restore unchanged.');
  assert.equal(await rootValue(notificationSnapshotQuery), notificationSnapshot);
  assert.equal(await rootValue(paymentSnapshotQuery), paymentSnapshot);
  await record('S6: notification IDs, snapshots, read state, inbox dedupe, delivery state and migration checksums survived volume restart and logical restore.');
  await record('S8: payment IDs, amounts, provider references, receipts, audit rows and copy marker survived logical restore.');
  await record('Logical backup restored to all four isolated owner schemas; Flyway/Hibernate startup and data parity passed.');

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
    await sql(`CREATE TABLE \`${identitySchema}\`.e2e_guard (token VARCHAR(64) NOT NULL); INSERT INTO \`${identitySchema}\`.e2e_guard VALUES ('${token}');`, { rootUser: true });
    const controlPort = await freePort(); env.E2E_CONTROL_URL = `http://127.0.0.1:${controlPort}`;
    const identityControlPort = await freePort(); env.E2E_IDENTITY_CONTROL_URL = `http://127.0.0.1:${identityControlPort}`;
    config.services.backend.build = { context: resolve(root, 'back'), target: 'e2e' };
    config.services.backend.ports = [`127.0.0.1:${controlPort}:8080`];
    Object.assign(config.services.backend.environment, {
      E2E_DATABASE: schema,
      E2E_CONTROL_TOKEN: token,
      DB_PASSWORD: env.MYSQL_PASSWORD,
      IDENTITY_URL: 'http://identity:8080',
      IDENTITY_BACKEND_SECRET: env.IDENTITY_BACKEND_SECRET,
      IDENTITY_VERIFICATION_JWKS: env.IDENTITY_VERIFICATION_JWKS,
    });
    config.services.identity = config.services.identity || {};
    config.services.identity.build = { context: resolve(root, 'services/identity-service'), target: 'e2e' };
    config.services.identity.ports = [`127.0.0.1:${identityControlPort}:8080`];
    config.services.identity.environment = {
      E2E_IDENTITY_DATABASE: identitySchema,
      E2E_CONTROL_TOKEN: token,
      PUBLIC_URL: env.PUBLIC_URL,
      IDENTITY_DB_USERNAME: env.IDENTITY_DB_USERNAME,
      IDENTITY_DB_PASSWORD: env.IDENTITY_DB_PASSWORD,
      IDENTITY_SIGNING_JWK: env.IDENTITY_SIGNING_JWK,
      IDENTITY_GATEWAY_SECRET: env.IDENTITY_GATEWAY_SECRET,
      IDENTITY_BACKEND_SECRET: env.IDENTITY_BACKEND_SECRET,
    };
    await saveConfig();
    const notificationControlPort = await freePort();
    env.E2E_NOTIFICATION_CONTROL_URL = `http://127.0.0.1:${notificationControlPort}`;
    await sql(`CREATE TABLE \`${notificationSchema}\`.e2e_guard (token VARCHAR(64) NOT NULL); INSERT INTO \`${notificationSchema}\`.e2e_guard VALUES ('${token}');`, { rootUser: true });
    config.services.notification = {
      build: { context: resolve(root, 'services/notification-service'), target: 'e2e' },
      ports: [`127.0.0.1:${notificationControlPort}:8080`],
      environment: { E2E_NOTIFICATION_DATABASE: notificationSchema, E2E_CONTROL_TOKEN: token },
    };
    const paymentControlPort = await freePort();
    env.E2E_PAYMENT_CONTROL_URL = `http://127.0.0.1:${paymentControlPort}`;
    config.services.payment = {
      build: { context: resolve(root, 'services/payment-service'), target: 'e2e' },
      ports: [`127.0.0.1:${paymentControlPort}:8080`],
      environment: { E2E_CONTROL_TOKEN: token, E2E_PAYMENT_BROKER_ENABLED: 'true' },
    };
    await saveConfig();
    await compose(['build', 'identity', 'backend', 'notification', 'payment']);
    // Replace owner services before their consumers so Docker DNS never leaves a
    // newly started backend or gateway holding an address for a removed owner.
    await compose(['up', '--detach', '--wait', '--wait-timeout', '240',
      'identity', 'notification', 'payment'], { timeout: 300000 });
    await compose(['up', '--detach', '--wait', '--wait-timeout', '240'], { timeout: 300000 });
    await eventually(async () => {
      const response = await fetch(env.E2E_CONTROL_URL + '/__e2e/payment-ready', {
        headers: { 'X-E2E-Control': token }, signal: AbortSignal.timeout(3000) });
      return response.ok;
    }, 'backend-to-payment capability routing');
    // A replaced owner's old Docker IP can be assigned to a different service.
    // Assert owner routing recovers before starting browser mutations; never replay a mutation.
    await eventually(async () => {
      const response = await fetch(env.PUBLIC_URL + '/api/csrf', { signal: AbortSignal.timeout(3000) });
      return response.ok && typeof (await response.json()).token === 'string';
    }, 'identity routing after owner container replacement');
    if (process.argv.includes('--verify-failure-cleanup')) throw new Error('Intentional post-startup failure to verify Compose cleanup');
    if (process.argv.includes('--verify-browser-failure')) env.E2E_FAILURE_PROBE = 'true';
    const result = await execute(process.execPath, ['node_modules/@playwright/test/cli.js', 'test',
      ...(env.E2E_FAILURE_PROBE ? ['--grep', 'register, reject bad login'] : process.argv.slice(2).filter(arg => !['--integration-only', '--verify-failure-cleanup'].includes(arg)))],
    { cwd: resolve(root, 'front'), allowFailure: true, timeout: 600000 });
    console.log(result.stdout);
    assert.equal(result.code, 0, 'Browser suite failed; see Playwright reports and Compose logs');
    await record('Browser scenarios passed through Compose gateway against isolated MySQL.');
  } else if (process.argv.includes('--verify-failure-cleanup')) {
    throw new Error('Intentional post-startup failure to verify Compose cleanup');
  }
} catch (error) {
  console.error(error.message); process.exitCode = 1;
  await captureFailureDiagnostics(error);
} finally {
  try { if (config) await cleanup(); } catch (error) { console.error(`Cleanup failed: ${error.message}`); process.exitCode = 1; }
}
