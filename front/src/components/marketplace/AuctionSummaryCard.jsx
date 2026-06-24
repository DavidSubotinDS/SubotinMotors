import { Clock3 } from 'lucide-react';

import { backendUrl } from '../../config.js';
import Card from '../ui/Card.jsx';
import VehicleImage from './VehicleImage.jsx';

export default function AuctionSummaryCard({ auction }) {
  return (
    <Card className="summary-card">
      <VehicleImage
        src={auction.imageUrl}
        alt={`${auction.make} ${auction.model}`}
        fallback={auction.make?.slice(0, 2) || 'AU'}
      />
      <div className="summary-card-body">
        <div className="summary-meta">
          <span>{auction.year}</span>
          <span>{auction.statusLabel}</span>
        </div>
        <h2>{auction.make} {auction.model}</h2>
        <p>{formatWholeCurrency(auction.price)}</p>
        <span className="inline-status">
          <Clock3 aria-hidden="true" size={16} />
          {auction.auctionEndTime}
        </span>
      </div>
      <a className="summary-link" href={backendUrl(`/cars/${auction.make}/${auction.model}/${auction.year}/${auction.id}`)}>
        Open auction
      </a>
    </Card>
  );
}

function formatWholeCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}
