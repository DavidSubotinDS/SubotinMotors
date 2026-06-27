import { afterEach, expect, test, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';

import VehicleCarousel from './VehicleCarousel.jsx';

afterEach(() => {
  vi.useRealTimers();
});

test('supports manual navigation and advances automatically', () => {
  vi.useFakeTimers();
  render(
    <VehicleCarousel
      images={['/front.jpg', '/side.jpg', '/interior.jpg']}
      alt="Demo vehicle"
      fallback="DV"
      intervalMs={1000}
    />,
  );

  const image = screen.getByRole('img');
  expect(image).toHaveAttribute('src', '/front.jpg');

  fireEvent.click(screen.getByRole('button', { name: 'Next image' }));
  expect(image).toHaveAttribute('src', '/side.jpg');

  act(() => vi.advanceTimersByTime(1000));
  expect(image).toHaveAttribute('src', '/interior.jpg');

  fireEvent.click(screen.getByRole('button', { name: 'Previous image' }));
  expect(image).toHaveAttribute('src', '/side.jpg');
});

test('keeps a single cover image free of carousel controls', () => {
  render(<VehicleCarousel src="/cover.jpg" alt="Single image vehicle" fallback="SV" />);

  expect(screen.getByRole('img')).toHaveAttribute('src', '/cover.jpg');
  expect(screen.queryByRole('button', { name: 'Next image' })).not.toBeInTheDocument();
});
