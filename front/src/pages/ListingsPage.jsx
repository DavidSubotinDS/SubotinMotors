import { useMemo, useState } from 'react';

import ListingSummaryCard from '../components/marketplace/ListingSummaryCard.jsx';
import Alert from '../components/ui/Alert.jsx';
import Button from '../components/ui/Button.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import LoadingState from '../components/ui/LoadingState.jsx';
import PageHeader from '../components/ui/PageHeader.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { getListings } from '../services/marketplaceApi.js';

export default function ListingsPage() {
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const params = useMemo(() => ({ keyword: submittedKeyword, size: 12 }), [submittedKeyword]);
  const { data, error, loading } = useAsync(() => getListings(params), [params]);

  return (
    <>
      <PageHeader eyebrow="Fixed price" title="Vehicle listings">
        Deposits, seller validation, and test rides continue through the Spring Boot backend.
      </PageHeader>

      <form className="toolbar" onSubmit={(event) => {
        event.preventDefault();
        setSubmittedKeyword(keyword);
      }}>
        <input
          aria-label="Search fixed-price listings"
          placeholder="Search title, make, model, or year"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <Button type="submit">Search</Button>
      </form>

      {loading && <LoadingState label="Loading listings" />}
      {error && <Alert>Check that the backend API is reachable.</Alert>}
      {data?.content?.length === 0 && <EmptyState title="No listings found" />}
      {data?.content?.length > 0 && (
        <div className="summary-grid">
          {data.content.map((listing) => (
            <ListingSummaryCard key={listing.id} listing={listing} />
          ))}
        </div>
      )}
    </>
  );
}
