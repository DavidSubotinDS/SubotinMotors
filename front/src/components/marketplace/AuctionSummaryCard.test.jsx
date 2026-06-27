import { act, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, expect, test, vi } from 'vitest';

import AuctionSummaryCard from './AuctionSummaryCard.jsx';

const now = new Date('2026-06-26T12:00:00Z').getTime();

afterEach(() => {
  vi.useRealTimers();
});

test('shows and updates the remaining time for an open auction', () => {
  vi.useFakeTimers();
  vi.setSystemTime(now);

  renderCard({ auctionEndTimeEpochMillis: now + 90_061_000 });

  expect(screen.getByText('1d 1h 1m 1s remaining')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Open auction' })).toBeInTheDocument();

  act(() => vi.advanceTimersByTime(1000));
  expect(screen.getByText('1d 1h 1m 0s remaining')).toBeInTheDocument();
});

test('marks completed auctions as ended and gives them a viewing action', () => {
  vi.useFakeTimers();
  vi.setSystemTime(now);

  renderCard({ auctionEndTimeEpochMillis: now - 1000 });

  expect(screen.getByText('Auction ended')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'View auction' })).toBeInTheDocument();
});

test('switches an auction to its completed state when the countdown reaches zero', () => {
  vi.useFakeTimers();
  vi.setSystemTime(now);

  renderCard({ auctionEndTimeEpochMillis: now + 1000 });
  expect(screen.getByRole('link', { name: 'Open auction' })).toBeInTheDocument();

  act(() => vi.advanceTimersByTime(1000));
  expect(screen.getByText('Auction ended')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'View auction' })).toBeInTheDocument();
});

function renderCard(overrides = {}) {
  const auction = {
    id: 42,
    make: 'Volkswagen',
    model: 'Golf',
    year: '2020',
    price: 19_900,
    statusLabel: 'Active',
    auctionEndTime: '27 Jun 2026, 13:01',
    auctionEndTimeEpochMillis: now + 90_061_000,
    imageUrl: null,
    ...overrides,
  };

  return render(
    <MemoryRouter>
      <AuctionSummaryCard auction={auction} />
    </MemoryRouter>,
  );
}
