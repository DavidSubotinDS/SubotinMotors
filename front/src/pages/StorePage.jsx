import { useMemo, useState } from 'react';

import PartSummaryCard from '../components/marketplace/PartSummaryCard.jsx';
import Alert from '../components/ui/Alert.jsx';
import Button from '../components/ui/Button.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import LoadingState from '../components/ui/LoadingState.jsx';
import PageHeader from '../components/ui/PageHeader.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { getParts } from '../services/marketplaceApi.js';

export default function StorePage() {
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const params = useMemo(() => ({ keyword: submittedKeyword, size: 12 }), [submittedKeyword]);
  const { data, error, loading } = useAsync(() => getParts(params), [params]);

  return (
    <>
      <PageHeader eyebrow="Store" title="Car parts catalog">
        Inventory, cart, checkout, and signed Stripe webhooks stay server-side.
      </PageHeader>

      <form className="toolbar" onSubmit={(event) => {
        event.preventDefault();
        setSubmittedKeyword(keyword);
      }}>
        <input
          aria-label="Search car parts"
          placeholder="Search name, SKU, or description"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <Button type="submit">Search</Button>
      </form>

      {loading && <LoadingState label="Loading parts" />}
      {error && <Alert>Check that the backend API is reachable.</Alert>}
      {data?.content?.length === 0 && <EmptyState title="No parts found" />}
      {data?.content?.length > 0 && (
        <div className="part-grid">
          {data.content.map((part) => (
            <PartSummaryCard key={part.id} part={part} />
          ))}
        </div>
      )}
    </>
  );
}
