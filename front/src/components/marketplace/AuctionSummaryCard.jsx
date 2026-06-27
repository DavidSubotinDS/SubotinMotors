import { useEffect, useState } from 'react';
import { Clock3 } from 'lucide-react';
import { Link } from 'react-router-dom';

import { moneyWhole } from '../../utils/format.js';
import Card from '../ui/Card.jsx';
import VehicleImage from './VehicleImage.jsx';

export default function AuctionSummaryCard({ auction }) {
  const now = useCurrentTime();
  const end = Number(auction.auctionEndTimeEpochMillis);
  const hasEnded = Number.isFinite(end) && end > 0 && end <= now;

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
          Ends {auction.auctionEndTime}
        </span>
        <AuctionCountdown endTime={end} now={now} />
      </div>
      <Link className="summary-link" to={`/auctions/${auction.id}`}>
        {hasEnded ? 'View auction' : 'Open auction'}
      </Link>
    </Card>
  );
}

function AuctionCountdown({ endTime, now }) {
  const end = Number(endTime);

  if (!Number.isFinite(end) || end <= 0) {
    return null;
  }

  const remaining = end - now;
  if (remaining <= 0) {
    return <span className="auction-countdown is-ended">Auction ended</span>;
  }

  const totalSeconds = Math.floor(remaining / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const urgency = remaining <= 60 * 60 * 1000
    ? 'is-urgent'
    : remaining <= 24 * 60 * 60 * 1000 ? 'is-ending-soon' : '';

  return (
    <span className={`auction-countdown ${urgency}`} aria-live="off">
      {days}d {hours}h {minutes}m {seconds}s remaining
    </span>
  );
}

function useCurrentTime() {
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    const interval = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(interval);
  }, []);

  return now;
}
