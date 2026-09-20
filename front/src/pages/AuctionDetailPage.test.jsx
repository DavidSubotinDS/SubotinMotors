import { afterEach, expect, test, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';

import { routes } from '../router.jsx';

afterEach(() => {
  vi.restoreAllMocks();
});

test('explains an invalid bid and submits a valid bid', async () => {
  let highestBid = 20_750;
  const bidRequests = [];

  vi.spyOn(globalThis, 'fetch').mockImplementation(async (request, options = {}) => {
    const url = request.toString();
    if (url.includes('/api/csrf')) return jsonResponse({ token: 'component-fixture' });
    if (url.includes('/api/session')) {
      return jsonResponse({ authenticated: true, username: 'demo_bidder', roles: ['ROLE_USER'] });
    }
    if (url.includes('/api/public/auctions/5')) {
      return jsonResponse({
        auction: {
          id: 5,
          make: 'Autostrada Auctions',
          model: 'Demo Vehicle',
          year: '2020',
          price: 19_900,
          status: 'ACTIVE',
          statusLabel: 'Active',
          auctionEndTime: '01 Jul 2026, 17:35',
          auctionEndTimeEpochMillis: 1_783_010_100_000,
          imageUrl: null,
          sellerDisplayName: 'Ana Trgovic',
        },
        highestBid,
        following: true,
        comments: [],
      });
    }
    if (url.includes('/api/user/auctions/5/bid')) {
      bidRequests.push(JSON.parse(options.body));
      if (bidRequests.at(-1).bidPrice === 21_000) {
        return jsonResponse({
          message: 'Bid must be greater than the listed price and current highest bid',
          fieldErrors: {},
        }, 400);
      }
      highestBid = bidRequests.at(-1).bidPrice;
      return jsonResponse({ message: 'Bid placed.' });
    }
    return jsonResponse({}, 404);
  });

  const router = createMemoryRouter(routes, { initialEntries: ['/auctions/5'] });
  render(<RouterProvider router={router} />);

  const input = await screen.findByRole('spinbutton', { name: 'Bid amount' });
  expect(input).toHaveAttribute('min', '20751');

  fireEvent.change(input, { target: { value: '20000' } });
  fireEvent.click(screen.getByRole('button', { name: 'Bid' }));

  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Enter a whole-number bid of at least €20,751.',
  );
  expect(bidRequests).toHaveLength(0);

  fireEvent.change(input, { target: { value: '21000' } });
  fireEvent.click(screen.getByRole('button', { name: 'Bid' }));

  await waitFor(() => expect(bidRequests).toEqual([{ bidPrice: 21_000 }]));
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Bid must be greater than the listed price and current highest bid',
  );

  fireEvent.change(input, { target: { value: '22000' } });
  fireEvent.click(screen.getByRole('button', { name: 'Bid' }));

  await waitFor(() => expect(bidRequests).toEqual([{ bidPrice: 21_000 }, { bidPrice: 22_000 }]));
  expect(await screen.findByRole('status')).toHaveTextContent('Bid placed.');
  await waitFor(() => expect(screen.getByText('€22,000')).toBeInTheDocument());
});

function jsonResponse(body, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
}
