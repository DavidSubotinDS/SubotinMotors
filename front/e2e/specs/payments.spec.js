import { test, expect, login, fill, json, address, saveAddress, startStoreCheckout,
  signedEvent, sendEvent, providerPage, backend } from '../fixtures.js';

test('shipping address, cart, signed payment result, order snapshots and admin item details', async ({ page, fixtures }) => {
  await login(page);
  await page.goto('/parts/1');
  for (let i = 0; i < 2; i++) {
    const added = page.waitForResponse((response) => response.url().endsWith('/api/store/cart/items') && response.request().method() === 'POST');
    await page.getByRole('button', { name: 'Add to cart', exact: true }).click();
    expect((await added).status()).toBe(200);
  }
  await page.goto('/cart');
  await expect(page.getByText('Shipping address needed', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Checkout', exact: true })).toBeDisabled();
  expect((await page.request.post('/api/store/checkout')).status()).toBe(400);
  const item = (await json(page, '/api/store/cart')).items[0];
  await login(page, 'other');
  expect((await page.request.post(`/api/store/cart/items/${item.idCartItem}/remove`)).status()).toBe(404);
  await login(page);
  await page.goto('/user/profile/edit');
  await fill(page, address);
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Buyer Fixture', exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByText('12 Test Street, 21000 Novi Sad, Serbia', { exact: true })).toBeVisible();
  const session = await startStoreCheckout(page);
  const lookup = `/api/store/checkout/success?session_id=${session}`;
  let order = await json(page, lookup);
  expect(order).toMatchObject({ status: 'CHECKOUT_CREATED', totalMinor: 5000, paidAt: null });
  expect(order.items).toHaveLength(1);
  expect(order.items[0]).toMatchObject({ sku: 'E2E-FILTER', partName: 'E2E Oil Filter', quantity: 2, unitPriceMinor: 2500, lineTotalMinor: 5000 });
  expect((await json(page, '/api/store/cart')).items).toHaveLength(0);
  expect((await json(page, '/api/public/parts/1')).part.stockQuantity).toBe(8);
  await page.getByRole('link', { name: 'Return without payment' }).click();
  await expect(page.getByRole('heading', { name: 'Checkout received', exact: true })).toBeVisible();
  await page.reload();
  expect((await json(page, lookup)).status).toBe('CHECKOUT_CREATED');
  const event = signedEvent(session, fixtures.stripeApiVersion);
  await sendEvent(page, { ...event, data: event.data.replace('"paid"', '"unpaid"') }, 400);
  await sendEvent(page, { ...event, headers: { ...event.headers, 'Stripe-Signature': 't=1,v1=invalid' } }, 400);
  expect((await json(page, lookup)).status).toBe('CHECKOUT_CREATED');
  await sendEvent(page, event);
  order = await json(page, lookup);
  expect(order.status).toBe('PAID');
  expect(order.paidAt).toBeTruthy();
  await sendEvent(page, event);
  expect((await json(page, lookup)).paidAt).toBe(order.paidAt);
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Payment confirmed', exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'View order', exact: true }).click();
  await expect(page.getByRole('heading', { name: `Order #${order.idOrder}` })).toBeVisible();
  await expect(page.getByRole('row').filter({ hasText: 'E2E-FILTER' })).toContainText('E2E Oil Filter');
  // Editing today's profile must not rewrite the order's shipping snapshot.
  await expect(await page.request.put('/api/user/profile', { data: { ...address, streetAddress: '99 Changed Street' } })).toBeOK();
  expect((await json(page, `/api/store/orders/${order.idOrder}`)).shippingAddress).toBe(order.shippingAddress);
  await login(page, 'other');
  expect((await page.request.get(lookup)).status()).toBe(404);
  expect((await page.request.get(`/api/store/orders/${order.idOrder}`)).status()).toBe(404);
  await page.goto(`/orders/${order.idOrder}`);
  await expect(page.getByText('Order unavailable', { exact: true })).toBeVisible();
  await login(page, 'admin');
  await page.goto('/admin/store/orders');
  const adminRow = page.getByRole('row').filter({ hasText: `#${order.idOrder}` });
  await adminRow.getByRole('link', { name: `#${order.idOrder}`, exact: true }).click();
  await expect(page.getByRole('heading', { name: `Order #${order.idOrder}` })).toBeVisible();
  await expect(page.getByRole('row').filter({ hasText: 'E2E-FILTER' })).toContainText('E2E Oil Filter');
  expect(await json(page, `/api/admin/store/orders/${order.idOrder}`)).toMatchObject({
    totalMinor: 5000, shippingAddress: order.shippingAddress, items: order.items, status: 'PAID',
  });
});

test('cancel return and unpaid completion never pay; signed expiry restores stock and displays expired', async ({ page, fixtures }) => {
  await login(page);
  await saveAddress(page);
  await expect(await page.request.post('/api/store/cart/items', { data: { idPart: 1, quantity: 1 } })).toBeOK();
  const session = await startStoreCheckout(page);
  const lookup = `/api/store/checkout/success?session_id=${session}`;
  await page.getByRole('link', { name: 'Cancel checkout' }).click();
  await expect(page).toHaveURL(/\/cart\?checkoutCanceled/);
  await expect(page.getByText('Your cart is empty.', { exact: true })).toBeVisible();
  expect((await json(page, lookup))).toMatchObject({ status: 'CHECKOUT_CREATED', paidAt: null });
  expect((await json(page, '/api/public/parts/1')).part.stockQuantity).toBe(9);
  await sendEvent(page, signedEvent(session, fixtures.stripeApiVersion, 'checkout.session.completed', 'unpaid', 'evt_e2e_unpaid'));
  await page.goto(`${backend}/store/checkout/success?session_id=${session}`);
  await expect(page.getByRole('heading', { name: 'Checkout received', exact: true })).toBeVisible();
  await expect(page.getByText('Your order is being confirmed.', { exact: true })).toBeVisible();
  expect((await json(page, lookup))).toMatchObject({ status: 'CHECKOUT_CREATED', paidAt: null });
  const expiry = signedEvent(session, fixtures.stripeApiVersion, 'checkout.session.expired', 'unpaid', 'evt_e2e_expired');
  await sendEvent(page, expiry);
  await sendEvent(page, expiry);
  const order = await json(page, lookup);
  expect(order).toMatchObject({ status: 'EXPIRED', paidAt: null });
  expect((await json(page, '/api/public/parts/1')).part.stockQuantity).toBe(10);
  await page.goto('/orders');
  await expect(page.getByRole('row').filter({ hasText: `#${order.idOrder}` })).toContainText('EXPIRED');
  await page.goto(`/orders/${order.idOrder}`);
  await expect(page.getByText('EXPIRED', { exact: true })).toBeVisible();
});

test('listing deposit return is read-only; signed success reserves the listing without selling it', async ({ page, fixtures }) => {
  await login(page, 'seller');
  expect((await page.request.post('/api/user/listings/1/deposit')).status()).toBe(400);
  await login(page);
  await providerPage(page, { deposit: true });
  await page.goto('/listings/1');
  await page.getByRole('button', { name: 'Place deposit', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Simulated provider checkout' })).toBeVisible();
  const session = new URL(page.url()).searchParams.get('session_id');
  const lookup = `/api/user/listing-deposits/success?session_id=${session}`;
  await page.getByRole('link', { name: 'Return without payment' }).click();
  await expect(page.getByRole('heading', { name: 'Deposit received', exact: true })).toBeVisible();
  expect((await json(page, lookup))).toMatchObject({ status: 'CHECKOUT_CREATED', paidAt: null });
  expect((await json(page, '/api/public/listings/1')).listing.status).toBe('RESERVED');
  await login(page, 'other');
  expect((await page.request.get(lookup)).status()).toBe(404);
  expect((await page.request.post('/api/user/listings/1/deposit')).status()).toBe(400);
  await login(page);
  const event = signedEvent(session, fixtures.stripeApiVersion, 'checkout.session.completed', 'paid', 'evt_e2e_deposit');
  await sendEvent(page, event);
  await sendEvent(page, event);
  await page.goto(`${backend}/listing-deposits/success?session_id=${session}`);
  await expect(page).toHaveURL(new RegExp(`15173/listing-deposits/success\\?session_id=${session}`));
  await expect(page.getByRole('heading', { name: 'Deposit confirmed', exact: true })).toBeVisible();
  expect((await json(page, lookup)).status).toBe('PAID');
  expect((await json(page, '/api/public/listings/1')).listing.status).toBe('RESERVED');
  await page.getByRole('link', { name: 'View deposits', exact: true }).click();
  await expect(page.getByRole('row').filter({ hasText: 'E2E Touring' })).toContainText('PAID');
});

test('deposit cancel leaves reservation pending until a signed expiry releases it', async ({ page, fixtures }) => {
  await login(page);
  await providerPage(page, { deposit: true });
  await page.goto('/listings/1');
  await page.getByRole('button', { name: 'Place deposit', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Simulated provider checkout' })).toBeVisible();
  const session = new URL(page.url()).searchParams.get('session_id');
  await page.getByRole('link', { name: 'Cancel checkout' }).click();
  await expect(page).toHaveURL(/\/listings\/1\?depositCanceled/);
  await expect(page.getByText('RESERVED', { exact: true })).toBeVisible();
  expect((await json(page, `/api/user/listing-deposits/success?session_id=${session}`))).toMatchObject({ status: 'CHECKOUT_CREATED', paidAt: null });
  const event = signedEvent(session, fixtures.stripeApiVersion, 'checkout.session.expired', 'unpaid', 'evt_e2e_deposit_expired');
  await sendEvent(page, event);
  await sendEvent(page, event);
  await page.reload();
  await expect(page.getByText('ACTIVE', { exact: true })).toBeVisible();
  await page.goto('/user/listing-deposits');
  await expect(page.getByRole('row').filter({ hasText: 'E2E Touring' })).toContainText('EXPIRED');
});
