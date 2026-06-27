import { afterEach, expect, test, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';

import { routes } from '../router.jsx';

afterEach(() => {
  vi.restoreAllMocks();
});

test('pending auctions link to a preview and visibility actions match each state', async () => {
  mockApi((url) => {
    if (url.includes('/api/admin/cars')) {
      return jsonResponse({
        cars: page([
          auction(12, 'Pending', 'Roadster', 'PENDING'),
          auction(11, 'Hidden', 'Coupe', 'DEACTIVE'),
          auction(10, 'Live', 'Wagon', 'ACTIVE'),
        ]),
        bids: page([]),
      });
    }
    return jsonResponse({}, 404);
  });

  renderRoute('/admin/cars');

  const previewLink = await screen.findByRole('link', { name: '2026 Pending Roadster' });
  expect(previewLink).toHaveAttribute('href', '/admin/cars/12/preview');
  expect(screen.getByRole('link', { name: 'Preview 2026 Pending Roadster' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Approve' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Show' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Hide' })).toBeInTheDocument();
});

test('admin auction preview renders the marketplace mockup and approval action', async () => {
  mockApi((url) => {
    if (url.includes('/api/admin/cars/12')) {
      return jsonResponse({
        auction: auction(12, 'Pending', 'Roadster', 'PENDING'),
        highestBid: 0,
        following: false,
        comments: [],
      });
    }
    return jsonResponse({}, 404);
  });

  renderRoute('/admin/cars/12/preview');

  expect(await screen.findByRole('heading', { name: 'Pending Roadster' })).toBeInTheDocument();
  expect(screen.getByText('Review the auction exactly as it will appear to marketplace visitors before approving it.')).toBeInTheDocument();
  expect(screen.getByText('Demo Seller')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Approve' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Back to moderation' })).toHaveAttribute('href', '/admin/cars');
});

test('hidden store parts remain listed with a Show action', async () => {
  mockApi((url) => {
    if (url.includes('/api/admin/store/parts')) {
      return jsonResponse(page([
        part(2, 'Hidden battery', false),
        part(1, 'Visible brakes', true),
      ]));
    }
    return jsonResponse({}, 404);
  });

  renderRoute('/admin/store/parts');

  expect(await screen.findByText('Hidden battery')).toBeInTheDocument();
  expect(screen.getByText('HIDDEN')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Show' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Hide' })).toBeInTheDocument();
});

function renderRoute(path) {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  render(<RouterProvider router={router} />);
}

function mockApi(handler) {
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (request) => {
    const url = request.toString();
    if (url.includes('/api/session')) {
      return jsonResponse({
        authenticated: true,
        username: 'admin123',
        displayName: 'Admin User',
        roles: ['ROLE_USER', 'ROLE_ADMIN'],
      });
    }
    return handler(url);
  });
}

function auction(id, make, model, status) {
  return {
    id,
    make,
    model,
    year: '2026',
    price: 25_000,
    status,
    statusLabel: status === 'PENDING' ? 'Pending approval' : status,
    auctionEndTime: '05 Jul 2026, 12:00',
    auctionEndTimeEpochMillis: 1_783_251_000_000,
    imageUrl: null,
    sellerDisplayName: 'Demo Seller',
  };
}

function part(id, name, active) {
  return {
    id,
    sku: `SKU-${id}`,
    name,
    category: 'Demo',
    description: 'Demo product',
    priceMinor: 9_999,
    stockQuantity: 5,
    imageUrl: null,
    active,
  };
}

function page(content) {
  return {
    content,
    number: 0,
    size: 10,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    first: true,
    last: true,
  };
}

function jsonResponse(body, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
}
