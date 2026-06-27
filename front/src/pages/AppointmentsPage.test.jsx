import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';
import { afterEach, expect, test, vi } from 'vitest';

import { AppointmentsPage } from './MigratedPages.jsx';

afterEach(() => {
  vi.restoreAllMocks();
});

test('opens the related auction when an auction appointment row is clicked', async () => {
  const router = renderAppointmentsPage();

  const row = (await screen.findByText('2020 Volkswagen Golf')).closest('tr');
  fireEvent.click(row);

  await waitFor(() => expect(router.state.location.pathname).toBe('/auctions/42'));
});

test('opens the related listing when a listing test ride row is clicked', async () => {
  const router = renderAppointmentsPage();

  const row = (await screen.findByText('Mercedes EQE 350')).closest('tr');
  fireEvent.click(row);

  await waitFor(() => expect(router.state.location.pathname).toBe('/listings/77'));
});

function renderAppointmentsPage() {
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (request) => {
    const url = request.toString();
    if (url.includes('/api/user/appointments')) {
      return jsonResponse({
        bookedTestDrives: [
          {
            idTestDrive: 101,
            date: '2026-07-01',
            status: 'PENDING',
            auction: {
              id: 42,
              make: 'Volkswagen',
              model: 'Golf',
              year: '2020',
              price: 19900,
              status: 'ACTIVE',
              statusLabel: 'Active',
              auctionEndTime: '01 Jul 2026, 17:35',
              auctionEndTimeEpochMillis: 1782920100000,
              imageUrl: null,
              sellerDisplayName: 'Demo Seller',
            },
            requester: { idUser: 3, username: 'demo_bidder' },
          },
        ],
        receivedTestDrives: [],
        listingTestRideRequests: [],
        listingTestRides: [
          {
            idTestRide: 202,
            scheduledAt: '2026-07-02T12:30:00',
            status: 'ACCEPTED',
            listing: {
              id: 77,
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
            requester: { idUser: 3, username: 'demo_bidder' },
          },
        ],
      });
    }
    return jsonResponse({}, 404);
  });

  const router = createMemoryRouter(
    [
      { path: '/user/appointments', element: <AppointmentsPage /> },
      { path: '/auctions/:id', element: <div>Auction detail</div> },
      { path: '/listings/:id', element: <div>Listing detail</div> },
    ],
    { initialEntries: ['/user/appointments'] },
  );

  render(<RouterProvider router={router} />);
  return router;
}

function jsonResponse(body, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
}
