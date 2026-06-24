import { Gauge } from 'lucide-react';

import { backendUrl } from '../../config.js';
import Card from '../ui/Card.jsx';
import VehicleImage from './VehicleImage.jsx';

export default function ListingSummaryCard({ listing }) {
  return (
    <Card className="summary-card">
      <VehicleImage
        src={listing.imageUrl}
        alt={listing.title}
        fallback={listing.make?.slice(0, 2) || 'FP'}
      />
      <div className="summary-card-body">
        <div className="summary-meta">
          <span>{listing.year}</span>
          <span>{listing.status}</span>
        </div>
        <h2>{listing.title}</h2>
        <p>{formatMinorCurrency(listing.priceMinor)}</p>
        <span className="inline-status">
          <Gauge aria-hidden="true" size={16} />
          {listing.mileage.toLocaleString()} km
        </span>
      </div>
      <a className="summary-link" href={backendUrl(`/listings/${listing.id}`)}>
        Open listing
      </a>
    </Card>
  );
}

function formatMinorCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'EUR',
  }).format((value ?? 0) / 100);
}
