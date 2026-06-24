import { getJson } from '../api/client.js';

export function getSession() {
  return getJson('/api/session');
}

export function getMarketplaceSummary() {
  return getJson('/api/public/summary');
}

export function getAuctions(params = {}) {
  return getJson('/api/public/auctions', params);
}

export function getListings(params = {}) {
  return getJson('/api/public/listings', params);
}

export function getParts(params = {}) {
  return getJson('/api/public/parts', params);
}
