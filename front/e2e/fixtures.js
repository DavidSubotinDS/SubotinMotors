import { test as base, expect } from '@playwright/test';
import { createHmac } from 'node:crypto';

export const backend = 'http://127.0.0.1:18080';
export const password = 'E2e-pass-123!';
export const address = { email: 'buyer@e2e.invalid', firstName: 'Buyer', lastName: 'Fixture', phoneNumber: '+381641234567',
  address: 'Novi Sad', streetAddress: '12 Test Street', city: 'Novi Sad', postalCode: '21000', country: 'Serbia', about: '' };
export const vehicleImage = { name: 'vehicle.png', mimeType: 'image/png',
  buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a2ioAAAAASUVORK5CYII=', 'base64') };

export const test = base.extend({
  fixtures: [async ({ playwright, page }, use) => {
    const control = await playwright.request.newContext({ baseURL: backend,
      extraHTTPHeaders: { 'X-E2E-Control': process.env.E2E_CONTROL_TOKEN } });
    const response = await control.post('/__e2e/reset');
    await expect(response).toBeOK();
    const data = await response.json();
    await page.clock.setFixedTime(new Date(data.instant));
    const errors = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await use({ ...data, setTime: async (instant) => {
      await expect(await control.post('/__e2e/clock', { data: { instant } })).toBeOK();
      await page.clock.setFixedTime(new Date(instant));
    } });
    await control.dispose();
    expect(errors, 'No uncaught browser exceptions').toEqual([]);
  }, { auto: true }],
});
export { expect };

export async function login(page, username = 'buyer') {
  await page.goto('/login');
  await page.getByLabel('Username', { exact: true }).fill(username);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
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
  await expect(await page.request.put('/api/user/profile', { data: address })).toBeOK();
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
  expect((await page.request.post(`${backend}/webhooks/stripe`, event)).status()).toBe(status);
}
