import { test as base, expect } from '@playwright/test';
import { createHmac } from 'node:crypto';

export const gateway = process.env.E2E_GATEWAY_URL || 'http://127.0.0.1:18081';
export const password = 'E2e-pass-123!';
export const address = { email: 'buyer@e2e.invalid', firstName: 'Buyer', lastName: 'Fixture', phoneNumber: '+381641234567',
  address: 'Novi Sad', streetAddress: '12 Test Street', city: 'Novi Sad', postalCode: '21000', country: 'Serbia', about: '' };
export const vehicleImage = { name: 'vehicle.png', mimeType: 'image/png',
  buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a2ioAAAAASUVORK5CYII=', 'base64') };

export const test = base.extend({
  fixtures: [async ({ playwright, page }, use) => {
    const control = await playwright.request.newContext({ baseURL: process.env.E2E_CONTROL_URL || 'http://127.0.0.1:18080',
      extraHTTPHeaders: { 'X-E2E-Control': process.env.E2E_CONTROL_TOKEN } });
    const identityControl = await playwright.request.newContext({ baseURL: process.env.E2E_IDENTITY_CONTROL_URL || 'http://127.0.0.1:18082',
      extraHTTPHeaders: { 'X-E2E-Control': process.env.E2E_CONTROL_TOKEN } });
    const identityReset = await identityControl.post('/__e2e/reset');
    await expect(identityReset).toBeOK();
    const response = await control.post('/__e2e/reset');
    await expect(response).toBeOK();
    const data = { ...(await identityReset.json()), ...(await response.json()) };
    await page.clock.setFixedTime(new Date(data.instant));
    const errors = [];
    const bypasses = [];
    page.on('request', request => {
      const url = new URL(request.url());
      if (['127.0.0.1', 'localhost', 'backend', 'frontend', 'mysql'].includes(url.hostname)
          && url.origin !== gateway) bypasses.push(request.url());
    });
    const probe = await page.request.get('/api/session');
    expect(probe.headers()['x-request-id'], 'Real gateway handled the API request').toMatch(/^[a-f0-9-]{36}$/);
    expect((await page.request.get('/__e2e/ready')).status()).toBe(404);
    page.on('pageerror', (error) => errors.push(error.message));
    if (process.env.E2E_FAILURE_PROBE === 'true') {
      await page.goto('/');
      throw new Error('Intentional browser failure to verify artifacts and Compose cleanup');
    }
    await use({ ...data, mail: async (recipient) => {
      const response = await identityControl.get('/__e2e/mail', { params: { recipient } });
      expect(response.status()).toBe(200);
      return (await response.json()).body;
    }, setTime: async (instant) => {
      await expect(await identityControl.post('/__e2e/clock', { data: { instant } })).toBeOK();
      await expect(await control.post('/__e2e/clock', { data: { instant } })).toBeOK();
      await page.clock.setFixedTime(new Date(instant));
    } });
    await identityControl.dispose();
    await control.dispose();
    expect(errors, 'No uncaught browser exceptions').toEqual([]);
    expect(bypasses, 'Browser must not bypass gateway').toEqual([]);
  }, { auto: true }],
});
export { expect };

export async function login(page, username = 'buyer') {
  await page.goto('/login');
  await page.getByLabel('Username', { exact: true }).fill(username);
  await page.getByLabel('Password', { exact: true }).fill(password);
  // Login navigates to / and explicitly reloads. Wait for that document before
  // a caller starts another navigation, especially when changing an existing user.
  const reloaded = page.waitForEvent('load');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await reloaded;
  await expect(page.getByRole('button', { name: 'Logout', exact: true })).toBeVisible();
  await expect.poll(async () => (await (await page.request.get('/api/session')).json()).username).toBe(username);
}

export async function fill(page, fields) {
  for (const [name, value] of Object.entries(fields)) {
    await page.locator(`[name="${name}"]`).fill(String(value));
  }
}

export async function json(page, path) {
  const response = await page.request.get(path);
  await expect(response).toBeOK();
  return response.json();
}

export async function saveAddress(page) {
  await expect(await mutate(page, 'PUT', '/api/user/profile', { data: address })).toBeOK();
}

// API setup/authorization probes must pass CSRF first. Negative CSRF scenarios
// deliberately use the raw request context instead. Never retries a mutation.
export async function mutate(page, method, path, options = {}) {
  const response = await page.request.get('/api/csrf');
  expect(response.status()).toBe(200);
  const { token } = await response.json();
  return page.request.fetch(path, { ...options, method,
    headers: { ...options.headers, 'X-CSRF-TOKEN': token } });
}

// Only this test-owned provider page is mocked; no application API response is intercepted.
export async function providerPage(page, { deposit = false, listingId = 1 } = {}) {
  await page.route('**/__provider/checkout?*', async (route) => {
    const id = new URL(route.request().url()).searchParams.get('session_id');
    const success = `${deposit ? '/listing-deposits/success' : '/store/checkout/success'}?session_id=${encodeURIComponent(id)}`;
    const cancel = deposit ? `/listings/${listingId}?depositCanceled` : '/cart?checkoutCanceled';
    await route.fulfill({ contentType: 'text/html', body: `<h1>Simulated provider checkout</h1>
      <a href="${success}">Return without payment</a><a href="${cancel}">Cancel checkout</a>` });
  });
}

export async function startStoreCheckout(page) {
  await providerPage(page);
  await page.goto('/cart');
  await page.getByRole('button', { name: 'Checkout', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Simulated provider checkout' })).toBeVisible();
  return new URL(page.url()).searchParams.get('session_id');
}

export function signedEvent(sessionId, apiVersion, type = 'checkout.session.completed', paid = 'paid', eventId = 'evt_e2e_paid') {
  const body = JSON.stringify({ id: eventId, object: 'event', api_version: apiVersion, type,
    data: { object: { id: sessionId, object: 'checkout.session', payment_status: paid, payment_intent: `pi_${eventId}` } } });
  // Stripe's real SDK verifies tolerance against wall time, independent of our business Clock.
  const timestamp = Math.floor(Date.now() / 1000);
  const signature = createHmac('sha256', 'whsec_e2e_public_fixture_secret').update(`${timestamp}.${body}`).digest('hex');
  return { data: body, headers: { 'Content-Type': 'application/json', 'Stripe-Signature': `t=${timestamp},v1=${signature}` } };
}

export async function sendEvent(page, event, status = 200) {
  const response = await page.request.post(`${gateway}/webhooks/stripe`, event);
  expect(response.headers()['x-request-id']).toMatch(/^[a-f0-9-]{36}$/);
  expect(response.status()).toBe(status);
}
