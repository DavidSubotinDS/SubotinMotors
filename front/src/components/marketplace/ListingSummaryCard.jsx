import { Gauge } from 'lucide-react';
import { Link } from 'react-router-dom';

import { moneyMinor } from '../../utils/format.js';
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
        <p>{moneyMinor(listing.priceMinor, 'EUR')}</p>
        <span className="inline-status">
          <Gauge aria-hidden="true" size={16} />
          {listing.mileage.toLocaleString()} km
        </span>
      </div>
      <Link className="summary-link" to={`/listings/${listing.id}`}>
        Open listing
      </Link>
    </Card>
  );
}
