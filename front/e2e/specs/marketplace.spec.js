import { test, expect, login, fill, json, password, vehicleImage, backend } from '../fixtures.js';

test('register, reject bad login, reuse the session across pages/reload, and invalidate logout', async ({ page }) => {
  expect((await page.request.get('/api/user/profile')).status()).toBe(401);
  await page.goto('/register');
  await fill(page, { username: 'newbuyer', email: 'newbuyer@e2e.invalid', password,
    firstName: 'New', lastName: 'Buyer', phoneNumber: '+381641234567', address: 'Novi Sad' });
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.getByRole('heading', { name: 'You are ready to sign in' })).toBeVisible();
  await page.goto('/login');
  await fill(page, { username: 'newbuyer', password: 'wrong-password' });
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByText('Sign in failed', { exact: true })).toBeVisible();
  await login(page, 'newbuyer');
  const cookie = (await page.context().cookies()).find((item) => item.name === 'JSESSIONID');
  expect(cookie?.httpOnly).toBe(true);
  await page.goto('/user/profile');
  await expect(page.getByRole('heading', { name: 'New Buyer', exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'New Buyer', exact: true })).toBeVisible();
  expect(await json(page, '/api/session')).toEqual({ authenticated: true, username: 'newbuyer', displayName: 'New Buyer', roles: ['ROLE_USER'] });
  await page.getByRole('button', { name: 'Logout', exact: true }).click();
  await expect(page.getByRole('link', { name: 'Login', exact: true })).toBeVisible();
  expect((await page.request.get('/api/user/profile')).status()).toBe(401);
  // Replaying the old cookie must not revive the invalidated server session.
  const replay = await page.request.get('/api/user/profile', { headers: { Cookie: `JSESSIONID=${cookie.value}` } });
  expect(replay.status()).toBe(401);
  await page.goto('/user/profile');
  await expect(page.getByText('Authentication required.', { exact: true })).toBeVisible();
});

test('USER cannot use admin routes or another seller’s auction/listing mutations', async ({ page }) => {
  expect((await page.request.get(`${backend}/__e2e/ready`, { maxRedirects: 0 })).status()).toBe(403);
  await login(page);
  await page.goto('/admin/store/orders');
  await expect(page.getByText('You do not have permission to perform this action.', { exact: true })).toBeVisible();
  for (const path of ['/api/admin/store/orders', '/api/admin/dashboard', '/api/user/auctions/1', '/api/user/listings/1']) {
    expect((await page.request.get(path)).status(), path).toBe(403);
  }
  for (const path of ['/api/user/auctions/1/deactivate', '/api/user/listings/1/deactivate', '/api/admin/cars/1/deactivate']) {
    expect((await page.request.post(path)).status(), path).toBe(403);
  }
  await page.goto('/user/auctions/1/edit');
  await expect(page.getByText('You do not have permission to perform this action.', { exact: true })).toBeVisible();
  expect((await json(page, '/api/public/auctions/1')).auction.status).toBe('ENDING_SOON');
  expect((await json(page, '/api/public/listings/1')).listing.status).toBe('ACTIVE');
  await login(page, 'admin');
  await page.goto('/admin/store/orders');
  await expect(page.getByRole('heading', { name: 'Store orders', exact: true })).toBeVisible();
  await expect(await page.request.get('/api/admin/dashboard')).toBeOK();
});

test('seller creates and edits an auction with an image; admin approves it', async ({ page }) => {
  await login(page, 'seller');
  await page.goto('/user/auctions/new');
  await fill(page, { make: 'Browser', model: 'Roadster', year: '2024', price: '12000', auctionEndTime: '2030-06-20T12:00' });
  await page.locator('input[type=file]').setInputFiles(vehicleImage);
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'My auctions', exact: true })).toBeVisible();
  const created = (await json(page, '/api/user/auctions')).find((car) => car.make === 'Browser');
  expect(created.status).toBe('PENDING');
  await page.goto(`/user/auctions/${created.id}/edit`);
  await expect(page.getByLabel('Model', { exact: true })).toHaveValue('Roadster');
  await fill(page, { model: 'Roadster Edited', auctionEndTime: '2030-06-21T12:00' });
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Browser Roadster Edited', exact: true })).toBeVisible();
  await login(page, 'admin');
  await page.goto('/admin/cars');
  const row = page.getByRole('link', { name: 'Preview 2024 Browser Roadster Edited', exact: true });
  await row.getByRole('button', { name: 'Approve', exact: true }).click();
  await expect(row).toContainText('ACTIVE');
  await page.goto(`/auctions/${created.id}`);
  await expect(page.getByRole('heading', { name: 'Browser Roadster Edited' })).toBeVisible();
  await expect(page.getByRole('img', { name: /Browser Roadster Edited/ }).first()).toBeVisible();
});

test('bid minimum, self-bid denial and exact auction deadline are enforced by the backend', async ({ page, fixtures }) => {
  await login(page, 'seller');
  await page.goto('/auctions/1');
  await page.getByRole('spinbutton', { name: 'Bid amount' }).fill('10001');
  await page.getByRole('button', { name: 'Bid', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('You cannot bid on your own car');
  await login(page);
  await page.goto('/auctions/1');
  await page.getByRole('spinbutton', { name: 'Bid amount' }).fill('10000');
  await page.getByRole('button', { name: 'Bid', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('whole-number bid');
  expect((await page.request.post('/api/user/auctions/1/bid', { data: { bidPrice: 10000 } })).status()).toBe(400);
  await fixtures.setTime('2030-06-15T11:59:59Z');
  await page.getByRole('spinbutton', { name: 'Bid amount' }).fill('10001');
  await page.getByRole('button', { name: 'Bid', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Bid');
  expect((await json(page, '/api/user/bids'))).toHaveLength(1);
  await fixtures.setTime('2030-06-15T12:00:00Z');
  await page.reload();
  await expect(page.getByText('Ended', { exact: true })).toBeVisible();
  await page.getByRole('spinbutton', { name: 'Bid amount' }).fill('10002');
  await page.getByRole('button', { name: 'Bid', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('This auction has ended');
  expect((await page.request.post('/api/user/auctions/1/test-drives', { data: { date: '2030-06-16' } })).status()).toBe(400);
  await fixtures.setTime('2030-06-15T12:00:01Z');
  expect((await page.request.post('/api/user/auctions/1/bid', { data: { bidPrice: 10003 } })).status()).toBe(400);
  expect((await json(page, '/api/user/bids'))).toHaveLength(1);
});

test('seller creates and edits a fixed-price listing', async ({ page }) => {
  await login(page, 'seller');
  await page.goto('/user/listings/new');
  await fill(page, { title: 'Browser Touring', make: 'Browser', model: 'Touring', year: '2024', mileage: '20000',
    fuelType: 'Petrol', transmission: 'Manual', price: '25000', depositAmount: '500', description: 'Browser fixture vehicle description.' });
  await page.locator('input[type=file]').setInputFiles(vehicleImage);
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'My listings', exact: true })).toBeVisible();
  const listing = (await json(page, '/api/user/listings')).find((item) => item.title === 'Browser Touring');
  await page.goto(`/user/listings/${listing.id}/edit`);
  await expect(page.getByLabel('Title', { exact: true })).toHaveValue('Browser Touring');
  await fill(page, { title: 'Browser Touring Updated' });
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('row').filter({ hasText: 'Browser Touring Updated' })).toContainText('ACTIVE');
  await page.goto(`/listings/${listing.id}`);
  await expect(page.getByRole('heading', { name: 'Browser Touring Updated', exact: true })).toBeVisible();
});

test('buyer requests both appointment types; only the seller accepts and the buyer cancels', async ({ page }) => {
  await login(page);
  await page.goto('/auctions/1');
  await page.locator('input[type=date]').fill('2030-06-16');
  const driveResponse = page.waitForResponse((response) => response.url().endsWith('/auctions/1/test-drives') && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Request test drive' }).click();
  expect((await driveResponse).status()).toBe(200);
  await page.goto('/listings/1');
  await page.locator('input[type=datetime-local]').fill('2030-06-16T12:00');
  const rideResponse = page.waitForResponse((response) => response.url().endsWith('/listings/1/test-rides') && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Request test ride' }).click();
  expect((await rideResponse).status()).toBe(200);
  const appointments = await json(page, '/api/user/appointments');
  const drive = appointments.bookedTestDrives[0].idTestDrive;
  const ride = appointments.listingTestRides[0].idTestRide;
  await login(page, 'other');
  for (const path of [`/api/user/test-drives/${drive}/accept`, `/api/user/listing-test-rides/${ride}/accept`,
    `/api/user/test-drives/${drive}/cancel`, `/api/user/listing-test-rides/${ride}/cancel`]) {
    expect((await page.request.post(path)).status()).toBe(403);
  }
  await login(page, 'seller');
  await page.goto('/user/appointments');
  for (const name of ['E2E Roadster', 'E2E Touring']) {
    const row = page.locator('tr').filter({ hasText: name });
    await row.getByRole('button', { name: 'Accept', exact: true }).click();
    await expect(row).toContainText('ACCEPTED');
  }
  await login(page);
  await page.goto('/user/appointments');
  for (const name of ['E2E Roadster', 'E2E Touring']) {
    const row = page.locator('tr').filter({ hasText: name });
    await expect(row).toContainText('ACCEPTED');
    await row.getByRole('button', { name: 'Cancel', exact: true }).click();
    await expect(row).toContainText('CANCELLED');
  }
});

test('follow generates ending-soon notifications; recipient read/read-all persist', async ({ page }) => {
  await login(page);
  for (const id of [1, 2]) {
    await page.goto(`/auctions/${id}`);
    await page.getByRole('button', { name: 'Follow', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Unfollow', exact: true })).toBeVisible();
  }
  await page.goto('/user/watchlist');
  await expect(page.getByRole('heading', { name: 'E2E Roadster', exact: true })).toBeVisible();
  const notifications = await json(page, '/api/user/notifications');
  expect(notifications).toHaveLength(2);
  await login(page, 'other');
  expect((await page.request.post(`/api/user/notifications/${notifications[0].idNotification}/read`)).status()).toBe(403);
  expect(await json(page, '/api/user/notifications')).toEqual([]);
  await login(page);
  await page.goto('/user/notifications');
  await expect(page.getByRole('cell', { name: 'Unread', exact: true })).toHaveCount(2);
  await page.getByRole('button', { name: 'Read', exact: true }).first().click();
  await expect(page.getByRole('cell', { name: 'Unread', exact: true })).toHaveCount(1);
  await page.reload();
  await expect(page.getByRole('cell', { name: 'Unread', exact: true })).toHaveCount(1);
  await page.getByRole('button', { name: 'Mark all read' }).click();
  await expect(page.getByRole('cell', { name: 'Read', exact: true })).toHaveCount(2);
  await page.reload();
  await expect(page.getByRole('button', { name: 'Read', exact: true })).toHaveCount(0);
});

test('legacy backend routes reach React and preserve search and checkout query parameters', async ({ page }) => {
  await login(page);
  await page.goto(`${backend}/cars?keyword=Roadster&sort=price&direction=asc`);
  await expect(page).toHaveURL(/15173\/auctions\?keyword=Roadster&sort=price&direction=asc/);
  await expect(page.getByRole('heading', { name: 'E2E Roadster', exact: true })).toBeVisible();
  for (const [path, target, heading] of [
    // Current legacy view resolver drops detail IDs. Characterize, do not claim parity.
    ['/car-listings/1', '/listings', 'Vehicle listings'],
    ['/store/parts/1', '/parts', 'Car parts catalog'],
    ['/user/my-auctions', '/user/auctions', 'My auctions'],
    ['/user/payments', '/orders', 'Orders'],
    ['/payments/success?session_id=forged', '/orders', 'Orders'],
    ['/payments/seller/onboarding', '/parts', 'Car parts catalog'],
  ]) {
    await page.goto(`${backend}${path}`);
    await expect(page).toHaveURL(`http://127.0.0.1:15173${target}`);
    await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
  }
  expect(await json(page, '/api/store/payments')).toMatchObject({ redirectUrl: '/orders' });
  await page.goto('/store/checkout/success?session_id=cs_e2e_unknown');
  await expect(page.getByText('Checkout lookup failed', { exact: true })).toBeVisible();
  // The React aliases themselves do retain detail IDs.
  await page.goto('/car-listings/1');
  await expect(page.getByRole('heading', { name: 'E2E Touring', exact: true })).toBeVisible();
  await page.goto('/store/parts/1');
  await expect(page.getByRole('heading', { name: 'E2E Oil Filter', exact: true })).toBeVisible();
});
