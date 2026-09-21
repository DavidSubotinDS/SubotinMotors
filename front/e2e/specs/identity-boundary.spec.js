import { test, expect, login, json, mutate, saveAddress, vehicleImage } from '../fixtures.js';

test('identity display refreshes across profile, auction and comments without exposing private fields', async ({ page }) => {
  await login(page, 'seller');
  const self = await json(page, '/api/user/profile');
  await expect(await mutate(page, 'PUT', '/api/user/profile', { data: {
    ...self, firstName: 'Updated', lastName: 'Seller', email: 'seller-private@e2e.invalid',
    phoneNumber: '+381641234567', streetAddress: 'Private Seller Street', city: 'Novi Sad',
    postalCode: '21000', country: 'Serbia', about: 'Public seller biography',
  } })).toBeOK();
  await expect(await mutate(page, 'POST', '/api/user/profile/picture', {
    multipart: { imageFile: vehicleImage },
  })).toBeOK();
  await expect(await mutate(page, 'POST', '/api/comments/cars/1', {
    multipart: { body: 'Identity boundary display check' },
  })).toBeOK();
  await expect(await mutate(page, 'POST', '/api/auth/logout')).toBeOK();
  const profile = await json(page, `/api/public/profiles/${self.idProfile}`);
  expect(profile.firstName).toBe('Updated');
  expect(profile.pictureDataUrl).toMatch(/^data:image\/png;base64,/);
  for (const field of ['email', 'phoneNumber', 'streetAddress', 'postalCode', 'address']) {
    expect(profile[field], field).toBeNull();
  }
  expect(profile.formattedShippingAddress).toBe('');
  const auctions = await json(page, `/api/public/profiles/${self.idProfile}/auctions`);
  expect(auctions.some(auction => auction.id === 1 && auction.sellerDisplayName === 'Updated Seller')).toBe(true);
  const detail = await json(page, '/api/public/auctions/1');
  expect(detail.comments).toEqual(expect.arrayContaining([
    expect.objectContaining({ authorName: 'Updated Seller', badgeLabel: 'Seller' }),
  ]));
  await page.goto(`/profiles/${self.idProfile}`);
  await expect(page.getByRole('heading', { name: 'Updated Seller', exact: true })).toBeVisible();
  await page.goto('/auctions/1');
  await expect(page.getByText('Identity boundary display check', { exact: true })).toBeVisible();
});

test('scalar cart and bid owners come from authentication despite forged owner fields', async ({ page }) => {
  await login(page);
  await saveAddress(page);
  const buyer = await json(page, '/api/user/profile');
  await expect(await mutate(page, 'POST', '/api/store/cart/items', {
    data: { idPart: 1, quantity: 2, userId: 2, ownerId: 2 },
  })).toBeOK();
  const item = (await json(page, '/api/store/cart')).items[0];
  await expect(await mutate(page, 'POST', '/api/user/auctions/1/bid', {
    data: { bidPrice: 12000, userId: 2, ownerId: 2 },
  })).toBeOK();
  const bid = (await json(page, '/api/user/bids'))[0];
  expect(bid.bidder.idUser).toBe(buyer.idUser);
  expect(bid.bidder.email).toBeNull();
  expect(bid.bidder.profile.streetAddress).toBeNull();
  await login(page, 'other');
  expect((await mutate(page, 'PUT', `/api/store/cart/items/${item.idCartItem}`, { data: { quantity: 9 } })).status()).toBe(404);
  expect((await mutate(page, 'POST', `/api/user/bids/${bid.idBid}/cancel`)).status()).toBe(403);
  await login(page);
  expect((await json(page, '/api/store/cart')).items[0].quantity).toBe(2);
  expect((await json(page, '/api/user/bids'))[0].status).toBe('ONGOING');
});
