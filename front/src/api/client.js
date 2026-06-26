import { API_BASE_URL } from '../config.js';

export class ApiError extends Error {
  constructor(message, status, fieldErrors = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export async function getJson(path, params = {}) {
  return requestJson(path, { params });
}

export async function postJson(path, body = {}, params = {}) {
  return requestJson(path, {
    method: 'POST',
    params,
    body: JSON.stringify(body),
    headers: { 'Content-Type': 'application/json' },
  });
}

export async function putJson(path, body = {}, params = {}) {
  return requestJson(path, {
    method: 'PUT',
    params,
    body: JSON.stringify(body),
    headers: { 'Content-Type': 'application/json' },
  });
}

export async function postForm(path, formData, params = {}) {
  return requestJson(path, {
    method: 'POST',
    params,
    body: formData,
  });
}

export async function putForm(path, formData, params = {}) {
  return requestJson(path, {
    method: 'PUT',
    params,
    body: formData,
  });
}

async function requestJson(path, options = {}) {
  const url = requestUrl(path);
  const params = options.params ?? {};
  Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .forEach(([key, value]) => url.searchParams.set(key, value));

  const response = await fetch(url, {
    method: options.method ?? 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...(options.headers ?? {}),
    },
    body: options.body,
  });

  if (!response.ok) {
    let payload = {};
    try {
      payload = await response.json();
    } catch {
      payload = {};
    }
    throw new ApiError(
      payload.message || `API request failed with status ${response.status}`,
      response.status,
      payload.fieldErrors || {},
    );
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

function requestUrl(path) {
  const safePath = path.startsWith('/') ? path : `/${path}`;
  return new URL(`${API_BASE_URL}${safePath}`, requestOrigin());
}

function requestOrigin() {
  return globalThis.location?.origin || 'http://localhost';
}
