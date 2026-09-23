// Native H2 harness only: simulated transport, production outbox/encryption/inbox code.
// Real RabbitMQ delivery/retry/permissions are a separate Compose acceptance gate.
export function messagePump(env) {
  let stopped = false;
  let failure;
  const headers = { 'X-E2E-Control': env.E2E_CONTROL_TOKEN };
  const instant = value => new Date(value).toISOString().replace('.000Z', 'Z');
  async function request(url, options = {}) {
    const response = await fetch(url, { ...options, headers: { ...headers, ...options.headers }, signal: AbortSignal.timeout(3000) });
    if (!response.ok) throw new Error('Private native message transport failed');
    return response;
  }
  const done = (async () => {
    while (!stopped) {
      try {
        for (const [base, sensitive] of [[env.E2E_CONTROL_URL, false], [env.E2E_IDENTITY_CONTROL_URL, true]]) {
          const rows = await (await request(`${base}/__e2e/outbox`)).json();
          for (const row of rows) {
            const id = row.event_id;
            const event = sensitive ? { eventId: id, eventType: 'identity.password-reset-delivery.v1', schemaVersion: 1,
              occurredAt: instant(row.created_at), producer: 'identity-service', aggregateType: 'PasswordResetDelivery',
              aggregateId: id, aggregateVersion: 1, correlationId: id, causationId: null,
              payload: { deliveryId: id, encryptedPayload: row.encrypted_payload, expiresAt: instant(row.expires_at) } } : JSON.parse(row.payload);
            await request(`${env.E2E_NOTIFICATION_CONTROL_URL}/__e2e/events?sensitive=${sensitive}`, {
              method: 'POST', headers: { 'Content-Type': 'application/octet-stream' }, body: JSON.stringify(event) });
            await request(`${base}/__e2e/outbox-ack`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ eventId: id }) });
          }
        }
      } catch { failure = new Error('Native test message transport failed; inspect private harness logs.'); }
      await new Promise(resolve => setTimeout(resolve, 100));
    }
  })();
  return { async stop() { stopped = true; await done; if (failure) throw failure; } };
}
