import { test, expect, login, mutate, json, password, saveAddress, vehicleImage, gateway, providerPage } from '../fixtures.js';

async function token(page) {
  const response = await page.request.get('/api/csrf');
  expect(response.status()).toBe(200);
  expect(response.headers()['cache-control']).toBe('no-store');
  const body = await response.json();
  expect(Object.keys(body)).toEqual(['token']);
  return body.token;
}
async function rejected(response) {
  expect(response.status()).toBe(403);
  expect((await response.json()).code).toBe('CSRF_INVALID');
}

for (const form of [false, true]) {
  test(`${form ? 'form' : 'API'} login rotates the session, rejects the old identifier and token, and keeps thank-you GET read-only`, async ({ page, playwright }) => {
    const before = await token(page);
    const oldCookie = (await page.context().cookies()).find(c => c.name === 'AUTOSTRADA_SESSION');
    const result = form
      ? await page.request.post('/loginUser', { form: { username: 'buyer', password, _csrf: before }, maxRedirects: 0 })
      : await page.request.post('/api/auth/login', { data: { username: 'buyer', password }, headers: { 'X-CSRF-TOKEN': before } });
    expect(result.status()).toBe(form ? 302 : 200);
    const currentCookie = (await page.context().cookies()).find(c => c.name === 'AUTOSTRADA_SESSION');
    expect(currentCookie.value !== oldCookie.value).toBe(true);
    const replay = await playwright.request.newContext({ baseURL: gateway, extraHTTPHeaders: { Cookie: `AUTOSTRADA_SESSION=${oldCookie.value}` } });
    try {
      expect((await (await replay.get('/api/session')).json()).authenticated).toBe(false);
      expect((await replay.get('/api/user/profile')).status()).toBe(401);
    } finally { await replay.dispose(); }
    await rejected(await page.request.post('/api/auth/logout', { headers: { 'X-CSRF-TOKEN': before } }));
    await page.goto('/register/thank-you');
    expect((await json(page, '/api/session')).username).toBe('buyer');
    await page.reload();
    expect((await json(page, '/api/session')).username).toBe('buyer');
    const fresh = await token(page);
    expect((await page.request.post('/api/auth/logout', { headers: { 'X-CSRF-TOKEN': fresh } })).status()).toBe(200);
    const after = await token(page);
    expect(after !== fresh).toBe(true);
    await rejected(await page.request.post('/api/auth/login', { data: { username: 'buyer', password }, headers: { 'X-CSRF-TOKEN': fresh } }));
    expect((await json(page, '/api/session')).authenticated).toBe(false);
  });
}

test('missing, invalid and foreign CSRF tokens cannot mutate JSON, form, multipart or public auth/reset state', async ({ page, playwright }) => {
  for (const path of ['/api/auth/login', '/api/auth/register', '/api/auth/password-reset', '/api/auth/password-reset/complete']) {
    await rejected(await page.request.post(path, { data: {} }));
  }
  await login(page);
  const other = await playwright.request.newContext({ baseURL: gateway });
  try {
    const foreign = (await (await other.get('/api/csrf')).json()).token;
    const before = await json(page, '/api/store/cart');
    const profile = await json(page, '/api/user/profile');
    for (const invalid of ['', 'invalid', foreign]) {
      const headers = invalid ? { 'X-CSRF-TOKEN': invalid } : {};
      await rejected(await page.request.post('/api/store/cart/items', { data: { idPart: 1, quantity: 1 }, headers }));
      expect((await page.request.post('/cart/items', { form: { idPart: 1, quantity: 1, _csrf: invalid }, maxRedirects: 0 })).status()).toBe(403);
      await rejected(await page.request.post('/api/user/profile/picture', { multipart: { imageFile: vehicleImage }, headers }));
      await rejected(await page.request.post('/api/user/auctions/1/bid', { data: { bidPrice: 10001 }, headers }));
      await rejected(await page.request.post('/api/user/listings/1/deposit', { headers }));
    }
    expect(await json(page, '/api/store/cart')).toEqual(before);
    expect(await json(page, '/api/user/profile')).toEqual(profile);
    expect(await json(page, '/api/user/bids')).toHaveLength(0);
    expect((await json(page, '/api/public/listings/1')).listing.status).toBe('ACTIVE');
    // Raw same-origin browser form also gets rejected, without CORS as a defense.
    const formStatus = await page.evaluate(async () => (await fetch('/cart/items', {
      method: 'POST', body: new URLSearchParams({ idPart: '1', quantity: '1' }),
    })).status);
    expect(formStatus).toBe(403);
    for (const path of ['/webhooks/stripe/extra', '/webhooks/stripe-like']) {
      expect((await page.request.post(path, { data: {} })).status()).toBe(404);
    }
    await rejected(await page.request.post('/api/webhooks/stripe', { data: {} }));
  } finally { await other.dispose(); }
});

test('stale checkout refreshes token and waits for explicit retry without creating a duplicate order', async ({ page }) => {
  await login(page);
  await saveAddress(page);
  await page.goto('/parts/1');
  await page.getByRole('button', { name: 'Add to cart', exact: true }).click();
  await expect.poll(async () => (await json(page, '/api/store/cart')).items.length).toBe(1);
  // Authentication in another client sharing this browser session invalidates the
  // token held by the current document, as another tab or session renewal would.
  await expect(await mutate(page, 'POST', '/api/auth/login', { data: { username: 'buyer', password } })).toBeOK();
  await providerPage(page);
  await page.getByRole('link', { name: 'Cart', exact: true }).first().click();
  let attempts = 0;
  page.on('request', request => { if (new URL(request.url()).pathname === '/api/store/checkout' && request.method() === 'POST') attempts++; });
  await page.getByRole('button', { name: 'Checkout', exact: true }).click();
  await expect(page.getByText(/The action was not performed/)).toBeVisible();
  expect(attempts).toBe(1);
  expect((await json(page, '/api/store/cart')).items).toHaveLength(1);
  expect((await json(page, '/api/store/orders')).content).toHaveLength(0);
  expect((await json(page, '/api/public/parts/1')).part.stockQuantity).toBe(10);
  await page.getByRole('button', { name: 'Checkout', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Simulated provider checkout' })).toBeVisible();
  expect(attempts).toBe(2);
  expect((await json(page, '/api/store/orders')).content).toHaveLength(1);
  expect((await json(page, '/api/public/parts/1')).part.stockQuantity).toBe(9);
});

test('expired session refreshes token without restoring authentication or replaying the action', async ({ page }) => {
  await login(page);
  await page.goto('/parts/1');
  await page.getByRole('button', { name: 'Add to cart', exact: true }).click();
  await expect.poll(async () => (await json(page, '/api/store/cart')).items.length).toBe(1);
  await expect(await mutate(page, 'POST', '/api/auth/logout')).toBeOK();
  await page.getByRole('button', { name: 'Add to cart', exact: true }).click();
  await expect(page.getByText(/The action was not performed/)).toBeVisible();
  expect((await json(page, '/api/session')).authenticated).toBe(false);
  await page.getByRole('button', { name: 'Add to cart', exact: true }).click();
  await expect(page.getByText('Authentication required.', { exact: true })).toBeVisible();
  await login(page);
  expect((await json(page, '/api/store/cart')).items[0].quantity).toBe(1);
});

test('password reset request and consumption use CSRF through the gateway and private test mailbox', async ({ page, fixtures }) => {
  await page.goto('/forgot-password');
  await page.locator('[name="identifier"]').fill('buyer');
  await page.getByRole('button', { name: /reset/i }).click();
  await expect(page.getByText('If an account matches that email or username, a reset link has been sent.', { exact: true })).toBeVisible();
  const mail = await fixtures.mail('buyer@e2e.invalid');
  const link = new URL(mail.match(/https?:\/\/\S+/)[0]);
  expect(link.origin).toBe(gateway);
  const reset = link.searchParams.get('token');
  await rejected(await page.request.post('/api/auth/password-reset/complete', {
    data: { token: reset, password: 'Changed-pass-123!', confirmPassword: 'Changed-pass-123!' },
  }));
  expect((await json(page, `/api/auth/password-reset/valid?token=${encodeURIComponent(reset)}`)).valid).toBe(true);
  await page.goto(link.toString());
  await page.locator('[name="password"]').fill('Changed-pass-123!');
  await page.locator('[name="confirmPassword"]').fill('Changed-pass-123!');
  await page.getByRole('button', { name: /password/i }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.getByLabel('Username', { exact: true }).fill('buyer');
  await page.getByLabel('Password', { exact: true }).fill('Changed-pass-123!');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Logout', exact: true })).toBeVisible();
  expect((await json(page, '/api/session')).username).toBe('buyer');
  expect((await json(page, `/api/auth/password-reset/valid?token=${encodeURIComponent(reset)}`)).valid).toBe(false);
});
