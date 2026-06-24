import { Link } from 'react-router-dom';

import EmptyState from '../components/ui/EmptyState.jsx';

export default function NotFoundPage() {
  return (
    <EmptyState title="Page not found">
      <Link to="/">Return to overview</Link>
    </EmptyState>
  );
}
