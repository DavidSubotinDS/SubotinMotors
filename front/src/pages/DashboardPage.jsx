import { Link } from 'react-router-dom';

import AuctionSummaryCard from '../components/marketplace/AuctionSummaryCard.jsx';
import ListingSummaryCard from '../components/marketplace/ListingSummaryCard.jsx';
import PartSummaryCard from '../components/marketplace/PartSummaryCard.jsx';
import Alert from '../components/ui/Alert.jsx';
import Button from '../components/ui/Button.jsx';
import Card from '../components/ui/Card.jsx';
import LoadingState from '../components/ui/LoadingState.jsx';
import PageHeader from '../components/ui/PageHeader.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { getMarketplaceSummary } from '../services/marketplaceApi.js';

export default function DashboardPage() {
  const { data, error, loading } = useAsync(getMarketplaceSummary, []);

  if (loading) {
    return <LoadingState label="Loading marketplace" />;
  }

  if (error) {
    return <Alert>Check that the Spring Boot backend is running.</Alert>;
  }

  return (
    <>
      <PageHeader
        eyebrow="Marketplace"
        title="Autostrada Auctions"
        actions={<Button href="/legacy" variant="secondary">JSP status</Button>}
      >
        Active auctions, fixed-price listings, and store inventory from the Spring Boot API.
      </PageHeader>

      <section className="metric-grid" aria-label="Marketplace counts">
        <MetricCard label="Auctions" value={data.featuredAuctions.length} tone="teal" />
        <MetricCard label="Fixed-price listings" value={data.fixedPriceListings.length} tone="indigo" />
        <MetricCard label="Store categories" value={data.partCategories.length} tone="amber" />
      </section>

      <SectionHeader title="Featured auctions" to="/auctions" />
      <div className="summary-grid">
        {data.featuredAuctions.map((auction) => (
          <AuctionSummaryCard key={auction.id} auction={auction} />
        ))}
      </div>

      <SectionHeader title="Fixed-price listings" to="/listings" />
      <div className="summary-grid">
        {data.fixedPriceListings.map((listing) => (
          <ListingSummaryCard key={listing.id} listing={listing} />
        ))}
      </div>

      <SectionHeader title="Parts store" to="/store" />
      <div className="part-grid">
        {data.storeParts.map((part) => (
          <PartSummaryCard key={part.id} part={part} />
        ))}
      </div>
    </>
  );
}

function MetricCard({ label, value, tone }) {
  return (
    <Card className={`metric-card metric-${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </Card>
  );
}

function SectionHeader({ title, to }) {
  return (
    <div className="section-header">
      <h2>{title}</h2>
      <Link to={to}>View all</Link>
    </div>
  );
}
