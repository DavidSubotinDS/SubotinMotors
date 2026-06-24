export const API_BASE_URL = normalizeBaseUrl(
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
);

export function backendUrl(path = '/') {
  const safePath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${safePath}`;
}

function normalizeBaseUrl(value) {
  return value.replace(/\/+$/, '');
}
