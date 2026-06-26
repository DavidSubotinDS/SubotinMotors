import { getJson, postForm, postJson, putForm, putJson } from '../api/client.js';

export const publicApi = {
  summary: () => getJson('/api/public/summary'),
  auctions: (params) => getJson('/api/public/auctions', params),
  auction: (id) => getJson(`/api/public/auctions/${id}`),
  listings: (params) => getJson('/api/public/listings', params),
  listing: (id) => getJson(`/api/public/listings/${id}`),
  parts: (params) => getJson('/api/public/parts', params),
  part: (id) => getJson(`/api/public/parts/${id}`),
  content: (page) => getJson(`/api/public/content/${page}`),
  profile: (id) => getJson(`/api/public/profiles/${id}`),
  profileAuctions: (id) => getJson(`/api/public/profiles/${id}/auctions`),
};

export const authApi = {
  login: (body) => postJson('/api/auth/login', body),
  logout: () => postJson('/api/auth/logout'),
  register: (body) => postJson('/api/auth/register', body),
  requestPasswordReset: (body) => postJson('/api/auth/password-reset', body),
  resetTokenValid: (token) => getJson('/api/auth/password-reset/valid', { token }),
  completePasswordReset: (body) => postJson('/api/auth/password-reset/complete', body),
};

export const userApi = {
  workspace: () => getJson('/api/user/workspace'),
  profile: () => getJson('/api/user/profile'),
  updateProfile: (body) => putJson('/api/user/profile', body),
  updateProfilePicture: (file) => postForm('/api/user/profile/picture', fileForm(file)),
  auctions: () => getJson('/api/user/auctions'),
  auction: (id) => getJson(`/api/user/auctions/${id}`),
  createAuction: (body, file) => postForm('/api/user/auctions', objectForm(body, { imageFile: file })),
  updateAuction: (id, body) => putJson(`/api/user/auctions/${id}`, body),
  uploadAuctionPicture: (id, file) => postForm(`/api/user/auctions/${id}/picture`, fileForm(file)),
  activateAuction: (id) => postJson(`/api/user/auctions/${id}/activate`),
  deactivateAuction: (id) => postJson(`/api/user/auctions/${id}/deactivate`),
  bid: (id, bidPrice) => postJson(`/api/user/auctions/${id}/bid`, { bidPrice }),
  scheduleTestDrive: (id, date) => postJson(`/api/user/auctions/${id}/test-drives`, { date }),
  followAuction: (id) => postJson(`/api/user/auctions/${id}/follow`),
  unfollowAuction: (id) => postJson(`/api/user/auctions/${id}/unfollow`),
  followedAuctions: () => getJson('/api/user/followed-auctions'),
  notifications: () => getJson('/api/user/notifications'),
  markNotificationRead: (id) => postJson(`/api/user/notifications/${id}/read`),
  markAllNotificationsRead: () => postJson('/api/user/notifications/read-all'),
  bids: () => getJson('/api/user/bids'),
  cancelBid: (id) => postJson(`/api/user/bids/${id}/cancel`),
  appointments: () => getJson('/api/user/appointments'),
  rescheduleTestDrive: (id, date) => postJson(`/api/user/test-drives/${id}/reschedule`, {}, { date }),
  cancelTestDrive: (id) => postJson(`/api/user/test-drives/${id}/cancel`),
  acceptTestDrive: (id) => postJson(`/api/user/test-drives/${id}/accept`),
  rejectTestDrive: (id) => postJson(`/api/user/test-drives/${id}/reject`),
  ownerCancelTestDrive: (id) => postJson(`/api/user/test-drives/${id}/owner-cancel`),
  listings: () => getJson('/api/user/listings'),
  listingForm: (id) => getJson(`/api/user/listings/${id}`),
  createListing: (body, file) => postForm('/api/user/listings', objectForm(body, { imageFile: file })),
  updateListing: (id, body, file) => putForm(`/api/user/listings/${id}`, objectForm(body, { imageFile: file })),
  activateListing: (id) => postJson(`/api/user/listings/${id}/activate`),
  deactivateListing: (id) => postJson(`/api/user/listings/${id}/deactivate`),
  listingDeposit: (id) => postJson(`/api/user/listings/${id}/deposit`),
  scheduleListingTestRide: (id, scheduledAt) => postJson(`/api/user/listings/${id}/test-rides`, { scheduledAt }),
  rescheduleListingTestRide: (id, scheduledAt) => postJson(`/api/user/listing-test-rides/${id}/reschedule`, {}, { scheduledAt }),
  cancelListingTestRide: (id) => postJson(`/api/user/listing-test-rides/${id}/cancel`),
  acceptListingTestRide: (id) => postJson(`/api/user/listing-test-rides/${id}/accept`),
  rejectListingTestRide: (id) => postJson(`/api/user/listing-test-rides/${id}/reject`),
  ownerCancelListingTestRide: (id) => postJson(`/api/user/listing-test-rides/${id}/owner-cancel`),
  listingDeposits: (page = 0) => getJson('/api/user/listing-deposits', { page }),
  listingDepositSuccess: (sessionId) => getJson('/api/user/listing-deposits/success', { session_id: sessionId }),
};

export const storeApi = {
  cart: () => getJson('/api/store/cart'),
  addToCart: (idPart, quantity = 1) => postJson('/api/store/cart/items', { idPart, quantity }),
  updateCartItem: (id, quantity) => putJson(`/api/store/cart/items/${id}`, { quantity }),
  removeCartItem: (id) => postJson(`/api/store/cart/items/${id}/remove`),
  checkout: () => postJson('/api/store/checkout'),
  checkoutSuccess: (sessionId) => getJson('/api/store/checkout/success', { session_id: sessionId }),
  orders: (page = 0) => getJson('/api/store/orders', { page }),
  order: (id) => getJson(`/api/store/orders/${id}`),
};

export const commentsApi = {
  addCarComment: (id, { body, imageFile }) => postForm(`/api/comments/cars/${id}`, objectForm({ body }, { imageFile })),
  addPartComment: (id, { body, imageFile }) => postForm(`/api/comments/parts/${id}`, objectForm({ body }, { imageFile })),
};

export const adminApi = {
  dashboard: (params) => getJson('/api/admin/dashboard', params),
  userProfile: (id) => getJson(`/api/admin/users/${id}`),
  updateUserProfile: (id, body) => putJson(`/api/admin/users/${id}`, body),
  markAdmin: (id) => postJson(`/api/admin/users/${id}/mark-admin`),
  cars: (params) => getJson('/api/admin/cars', params),
  activateCar: (id) => postJson(`/api/admin/cars/${id}/activate`),
  deactivateCar: (id) => postJson(`/api/admin/cars/${id}/deactivate`),
  approveBid: (id) => postJson(`/api/admin/bids/${id}/approve`),
  denyBid: (id) => postJson(`/api/admin/bids/${id}/deny`),
  transactions: (params) => getJson('/api/admin/transactions', params),
  storeParts: (params) => getJson('/api/admin/store/parts', params),
  storePart: (id) => getJson(`/api/admin/store/parts/${id}`),
  createStorePart: (body) => postJson('/api/admin/store/parts', body),
  updateStorePart: (id, body) => putJson(`/api/admin/store/parts/${id}`, body),
  setStorePartActive: (id, active) => postJson(`/api/admin/store/parts/${id}/active`, { active }),
  storeOrders: (params) => getJson('/api/admin/store/orders', params),
  storeOrder: (id) => getJson(`/api/admin/store/orders/${id}`),
};

function objectForm(values = {}, files = {}) {
  const formData = new FormData();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      formData.append(key, value);
    }
  });
  Object.entries(files).forEach(([key, value]) => {
    if (value) {
      formData.append(key, value);
    }
  });
  return formData;
}

function fileForm(file) {
  return objectForm({}, { imageFile: file });
}
