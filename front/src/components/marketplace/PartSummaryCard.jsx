import { PackageCheck } from 'lucide-react';
import { Link } from 'react-router-dom';

import { moneyMinor } from '../../utils/format.js';
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
        <p>{moneyMinor(part.priceMinor, 'EUR')}</p>
      </div>
      <Link className="summary-link" to={`/parts/${part.id}`}>
        Open part
      </Link>
    </Card>
  );
}
