import { afterEach, beforeEach, expect, test, vi } from 'vitest';

let client, fetchMock;
const response = (body, status = 200) => ({ ok: status >= 200 && status < 300, status, json: async () => body });
beforeEach(async () => {
  vi.resetModules();
  client = await import('./client.js');
  fetchMock = vi.spyOn(globalThis, 'fetch');
});
afterEach(() => vi.restoreAllMocks());
const paths = () => fetchMock.mock.calls.map(([url]) => new URL(url).pathname);

test('concurrent initial mutations share one acquisition; reads need no token', async () => {
  fetchMock.mockImplementation(async url => response(new URL(url).pathname === '/api/csrf' ? { token: 'first' } : {}));
  await client.getJson('/api/session');
  await Promise.all([client.postJson('/api/auth/register', { username: 'test' }), client.postJson('/api/auth/password-reset')]);
  expect(paths()).toEqual(['/api/session', '/api/csrf', '/api/auth/register', '/api/auth/password-reset']);
  expect(fetchMock.mock.calls[0][1].headers['X-CSRF-TOKEN']).toBeUndefined();
  expect(fetchMock.mock.calls[1][1]).toMatchObject({ credentials: 'include', cache: 'no-store' });
  for (const [, options] of fetchMock.mock.calls.slice(2)) expect(options.headers['X-CSRF-TOKEN']).toBe('first');
});

test('multipart and url-encoded bodies preserve browser content type and boundary handling', async () => {
  fetchMock.mockResolvedValueOnce(response({ token: 'upload' })).mockResolvedValue(response({}));
  const form = new FormData(); form.append('imageFile', new Blob(['image']), 'image.png');
  await client.postForm('/api/user/profile/picture', form);
  await client.putForm('/api/user/listings/1', form);
  await client.postForm('/legacy', new URLSearchParams({ field: 'value' }));
  for (const [, options] of fetchMock.mock.calls.slice(1)) {
    expect(options.headers['X-CSRF-TOKEN']).toBe('upload');
    expect(options.headers['Content-Type']).toBeUndefined();
  }
  expect(fetchMock.mock.calls[1][1].body).toBe(form);
});

test('login/logout refresh tokens before the next queued mutation', async () => {
  let version = 0;
  fetchMock.mockImplementation(async url => response(new URL(url).pathname === '/api/csrf' ? { token: `token${++version}` } : {}));
  await Promise.all([client.postJson('/api/auth/login'), client.postJson('/api/store/cart/items'), client.postJson('/api/auth/logout'), client.postJson('/api/auth/register')]);
  expect(paths()).toEqual(['/api/csrf', '/api/auth/login', '/api/csrf', '/api/store/cart/items', '/api/auth/logout', '/api/csrf', '/api/auth/register']);
  expect(fetchMock.mock.calls[3][1].headers['X-CSRF-TOKEN']).toBe('token2');
  expect(fetchMock.mock.calls[6][1].headers['X-CSRF-TOKEN']).toBe('token3');
});

test.each(['/api/store/checkout', '/api/user/listings/1/deposit', '/api/auth/login', '/api/user/profile/picture'])(
  'stale token refreshes but NEVER replays %s', async path => {
    fetchMock.mockResolvedValueOnce(response({ token: 'stale' }))
      .mockResolvedValueOnce(response({ code: 'CSRF_INVALID' }, 403))
      .mockResolvedValueOnce(response({ token: 'fresh' })).mockResolvedValueOnce(response({}));
    await expect(client.postJson(path)).rejects.toMatchObject({ code: 'CSRF_INVALID', status: 403 });
    expect(paths()).toEqual(['/api/csrf', path, '/api/csrf']);
    // Only this explicit subsequent user action sends a second mutation.
    await client.postJson(path);
    expect(fetchMock.mock.calls[3][1].headers['X-CSRF-TOKEN']).toBe('fresh');
  });

test.each([401, 403, 500, 502, 504])('ordinary %s errors do not trigger retries', async status => {
  fetchMock.mockResolvedValueOnce(response({ token: 'token' })).mockResolvedValueOnce(response({ message: 'Denied' }, status));
  await expect(client.postJson('/api/store/checkout')).rejects.toMatchObject({ status, message: 'Denied' });
  expect(paths()).toEqual(['/api/csrf', '/api/store/checkout']);
});

test('network ambiguity never retries a mutation and next user action acquires a token', async () => {
  fetchMock.mockResolvedValueOnce(response({ token: 'token' })).mockRejectedValueOnce(new Error('network'));
  await expect(client.postJson('/api/store/checkout')).rejects.toThrow('Check the current result');
  expect(paths()).toEqual(['/api/csrf', '/api/store/checkout']);
  fetchMock.mockResolvedValueOnce(response({ token: 'new' })).mockResolvedValueOnce(response({}));
  await client.postJson('/api/auth/login');
  expect(paths()[2]).toBe('/api/csrf');
});

test('failed initial acquisition sends no mutation and can recover', async () => {
  fetchMock.mockResolvedValueOnce(response({}, 503));
  await expect(client.postJson('/api/auth/login')).rejects.toMatchObject({ code: 'CSRF_UNAVAILABLE' });
  expect(paths()).toEqual(['/api/csrf']);
  fetchMock.mockResolvedValueOnce(response({ token: 'new' })).mockResolvedValue(response({}));
  await client.postJson('/api/auth/register');
  expect(paths()).toEqual(['/api/csrf', '/api/csrf', '/api/auth/register']);
});

test('a stale session cancels already queued mutations instead of sending them under a changed session', async () => {
  fetchMock.mockResolvedValueOnce(response({ token: 'old' }))
    .mockResolvedValueOnce(response({ code: 'CSRF_INVALID' }, 403)).mockResolvedValueOnce(response({ token: 'new' }));
  const results = await Promise.allSettled([client.postJson('/api/store/checkout'), client.postJson('/api/user/listings/1/deposit')]);
  expect(results[0].reason.code).toBe('CSRF_INVALID');
  expect(results[1].reason.code).toBe('SESSION_CHANGED');
  expect(paths()).toEqual(['/api/csrf', '/api/store/checkout', '/api/csrf']);
});

test('failure refreshing after successful authentication does not report login failure', async () => {
  fetchMock.mockResolvedValueOnce(response({ token: 'before' })).mockResolvedValueOnce(response({ authenticated: true }))
    .mockResolvedValueOnce(response({}, 503));
  await expect(client.postJson('/api/auth/login')).resolves.toEqual({ authenticated: true });
  fetchMock.mockResolvedValueOnce(response({ token: 'after' })).mockResolvedValueOnce(response({}));
  await client.postJson('/api/store/cart/items');
  expect(fetchMock.mock.calls[4][1].headers['X-CSRF-TOKEN']).toBe('after');
});

test('reload reacquires in memory without persistent storage', async () => {
  const local = vi.spyOn(Storage.prototype, 'setItem');
  fetchMock.mockImplementation(async url => response(new URL(url).pathname === '/api/csrf' ? { token: 'same-session' } : {}));
  await client.postJson('/api/auth/register');
  vi.resetModules();
  const reloaded = await import('./client.js');
  await reloaded.postJson('/api/auth/password-reset');
  expect(paths().filter(path => path === '/api/csrf')).toHaveLength(2);
  expect(local).not.toHaveBeenCalled();
  expect(fetchMock.mock.calls.every(([url]) => !new URL(url).search.includes('same-session'))).toBe(true);
});
