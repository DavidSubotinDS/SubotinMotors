import { API_BASE_URL } from '../config.js';

export class ApiError extends Error {
  constructor(message, status, fieldErrors = {}, code) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.code = code;
  }
}

export async function getJson(path, params = {}) {
  return requestJson(path, { params });
}

export async function postJson(path, body = {}, params = {}, headers = {}) {
  return requestJson(path, {
    method: 'POST',
    params,
    body: JSON.stringify(body),
    headers: { 'Content-Type': 'application/json', ...headers },
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

// Memory only. Serialize unsafe calls so login/logout and their token refresh
// cannot race another mutation in this tab. No mutation is automatically replayed.
let csrfToken;
let csrfAcquisition;
let mutations = Promise.resolve();
let sessionGeneration = 0;

async function acquireCsrf() {
  if (csrfToken) return csrfToken;
  if (!csrfAcquisition) {
    csrfAcquisition = (async () => {
      try {
        const response = await fetch(requestUrl('/api/csrf'), {
          credentials: 'include', cache: 'no-store', headers: { Accept: 'application/json' },
        });
        if (!response.ok) throw new Error();
        const payload = await response.json();
        if (typeof payload.token !== 'string' || !payload.token) throw new Error();
        csrfToken = payload.token;
        return csrfToken;
      } catch {
        throw new ApiError('Could not refresh your security token. Check your connection and try again.', 0, {}, 'CSRF_UNAVAILABLE');
      } finally {
        csrfAcquisition = undefined;
      }
    })();
  }
  return csrfAcquisition;
}

function requestJson(path, options = {}) {
  if (['GET', 'HEAD', 'OPTIONS'].includes(options.method ?? 'GET')) return performRequest(path, options);
  const generation = sessionGeneration;
  const pending = mutations.then(() => {
    if (generation !== sessionGeneration) {
      throw new ApiError('Your session changed. This queued action was not sent. Review the current page before trying again.', 0, {}, 'SESSION_CHANGED');
    }
    return performRequest(path, options, true);
  });
  mutations = pending.catch(() => {});
  return pending;
}

async function performRequest(path, options = {}, unsafe = false) {
  const url = requestUrl(path);
  const params = options.params ?? {};
  Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .forEach(([key, value]) => url.searchParams.set(key, value));

  const token = unsafe ? await acquireCsrf() : undefined;
  let response;
  try {
    response = await fetch(url, {
      method: options.method ?? 'GET',
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        ...(options.headers ?? {}),
        ...(unsafe ? { 'X-CSRF-TOKEN': token } : {}),
      },
      body: options.body,
    });
  } catch {
    // The server may have completed the operation, including an auth transition.
    csrfToken = undefined;
    sessionGeneration++;
    throw new ApiError('Connection lost. Check the current result before trying again; the request was not repeated.', 0);
  }

  if (!response.ok) {
    let payload;
    try {
      payload = await response.json();
    } catch {
      payload = {};
    }
    if (response.status === 403 && payload.code === 'CSRF_INVALID') {
      csrfToken = undefined;
      sessionGeneration++;
      await acquireCsrf();
      throw new ApiError('Your session security token changed. The action was not performed. Please try again; sign in if your session expired.', 403, {}, 'CSRF_INVALID');
    }
    if (response.status === 401) {
      csrfToken = undefined;
      sessionGeneration++;
    }
    throw new ApiError(
      payload.message || `API request failed with status ${response.status}`,
      response.status,
      payload.fieldErrors || {},
      payload.code,
    );
  }

  if (unsafe && ['/api/auth/login', '/api/auth/logout'].includes(path)) {
    csrfToken = undefined;
    // Authentication already succeeded. A failed read must not turn it into a
    // failed login/logout or suggest repeating it. Next mutation reacquires;
    // the existing UI navigates/reloads and re-reads /api/session.
    try { await acquireCsrf(); } catch { csrfToken = undefined; }
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
