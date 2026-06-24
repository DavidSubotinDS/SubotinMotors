import { useMemo, useState } from 'react';

import AuctionSummaryCard from '../components/marketplace/AuctionSummaryCard.jsx';
import Alert from '../components/ui/Alert.jsx';
import Button from '../components/ui/Button.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import LoadingState from '../components/ui/LoadingState.jsx';
import PageHeader from '../components/ui/PageHeader.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { getAuctions } from '../services/marketplaceApi.js';

export default function AuctionsPage() {
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const params = useMemo(() => ({ keyword: submittedKeyword, size: 12 }), [submittedKeyword]);
  const { data, error, loading } = useAsync(() => getAuctions(params), [params]);

  return (
    <>
      <PageHeader eyebrow="Auctions" title="Live vehicle auctions">
        Server-side auction rules remain authoritative for bidding and deadlines.
      </PageHeader>

      <form className="toolbar" onSubmit={(event) => {
        event.preventDefault();
        setSubmittedKeyword(keyword);
      }}>
        <input
          aria-label="Search auctions"
          placeholder="Search make, model, or year"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <Button type="submit">Search</Button>
      </form>

      {loading && <LoadingState label="Loading auctions" />}
      {error && <Alert>Check that the backend API is reachable.</Alert>}
      {data?.content?.length === 0 && <EmptyState title="No auctions found" />}
      {data?.content?.length > 0 && (
        <div className="summary-grid">
          {data.content.map((auction) => (
            <AuctionSummaryCard key={auction.id} auction={auction} />
          ))}
        </div>
      )}
    </>
  );
}
