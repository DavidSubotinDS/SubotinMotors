import { PackageCheck } from 'lucide-react';

import { backendUrl } from '../../config.js';
import Card from '../ui/Card.jsx';

export default function PartSummaryCard({ part }) {
  return (
    <Card className="part-card">
      <div className="part-thumb" aria-hidden="true">
        {part.imageUrl ? <img src={part.imageUrl} alt="" loading="lazy" /> : <PackageCheck size={28} />}
      </div>
      <div>
        <div className="summary-meta">
          <span>{part.category}</span>
          <span>{part.stockQuantity} in stock</span>
        </div>
        <h2>{part.name}</h2>
        <p>{formatMinorCurrency(part.priceMinor)}</p>
      </div>
      <a className="summary-link" href={backendUrl(`/parts/${part.id}`)}>
        Open part
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
