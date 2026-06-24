import { afterEach, expect, test, vi } from 'vitest';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';
import { render, screen, waitFor } from '@testing-library/react';

import { routes } from './router.jsx';

afterEach(() => {
  vi.restoreAllMocks();
});

test('renders marketplace data from the backend API', async () => {
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (request) => {
    const url = request.toString();
    if (url.includes('/api/session')) {
      return jsonResponse({ authenticated: false, roles: [] });
    }
    if (url.includes('/api/public/summary')) {
      return jsonResponse({
        featuredAuctions: [
          {
            id: 1,
            make: 'Audi',
            model: 'A6',
            year: '2021',
            price: 25000,
            status: 'ACTIVE',
            statusLabel: 'Active',
            auctionEndTime: '30 Jun 2026, 12:00',
            auctionEndTimeEpochMillis: 1782806400000,
            imageUrl: null,
            sellerDisplayName: 'Demo Seller',
          },
        ],
        fixedPriceListings: [
          {
            id: 10,
            title: 'Mercedes EQE 350',
            make: 'Mercedes',
            model: 'EQE',
            year: '2023',
            mileage: 12000,
            fuelType: 'Electric',
            transmission: 'Automatic',
            priceMinor: 5899000,
            depositAmountMinor: 100000,
            status: 'ACTIVE',
            imageUrl: null,
            sellerDisplayName: 'Demo Seller',
          },
        ],
        storeParts: [
          {
            id: 7,
            sku: 'BRK-PAD-001',
            name: 'Premium Ceramic Brake Pads',
            category: 'Brakes',
            description: 'Low-dust brake pad set.',
            priceMinor: 6999,
            stockQuantity: 24,
            imageUrl: null,
          },
        ],
        partCategories: ['Brakes'],
      });
    }
    return jsonResponse({}, 404);
  });

  const router = createMemoryRouter(routes, { initialEntries: ['/'] });
  render(<RouterProvider router={router} />);

  await waitFor(() => expect(screen.getByText('Audi A6')).toBeInTheDocument());
  expect(screen.getByText('Mercedes EQE 350')).toBeInTheDocument();
  expect(screen.getByText('Premium Ceramic Brake Pads')).toBeInTheDocument();
});

function jsonResponse(body, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
}
