import { Clock3 } from 'lucide-react';
import { Link } from 'react-router-dom';

import { moneyWhole } from '../../utils/format.js';
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
        <p>{moneyWhole(auction.price, 'EUR')}</p>
        <span className="inline-status">
          <Clock3 aria-hidden="true" size={16} />
          {auction.auctionEndTime}
        </span>
      </div>
      <Link className="summary-link" to={`/auctions/${auction.id}`}>
        Open auction
      </Link>
    </Card>
  );
}
