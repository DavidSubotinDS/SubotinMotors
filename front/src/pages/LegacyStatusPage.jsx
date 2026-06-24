import { ExternalLink } from 'lucide-react';

import { backendUrl } from '../config.js';
import Button from '../components/ui/Button.jsx';
import Card from '../components/ui/Card.jsx';
import PageHeader from '../components/ui/PageHeader.jsx';

const legacyAreas = [
  'Authentication and registration',
  'Profile and image uploads',
  'Bidding and payment workflows',
  'Cart and order checkout',
  'Admin moderation and store-order details',
];

export default function LegacyStatusPage() {
  return (
    <>
      <PageHeader eyebrow="Migration" title="JSP compatibility">
        React now owns the new frontend workspace. JSP remains available for flows that still need full replacement.
      </PageHeader>

      <Card className="legacy-card">
        <h2>Remaining JSP-backed flows</h2>
        <div className="legacy-list">
          {legacyAreas.map((area) => (
            <span key={area}>{area}</span>
          ))}
        </div>
        <div className="legacy-actions">
          <Button href={backendUrl('/')} icon={ExternalLink} variant="secondary">
            Open legacy home
          </Button>
          <Button href={backendUrl('/admin/dashboard')} icon={ExternalLink} variant="ghost">
            Open admin
          </Button>
        </div>
      </Card>
    </>
  );
}
