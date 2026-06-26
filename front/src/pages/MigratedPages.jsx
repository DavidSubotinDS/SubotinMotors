import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams, useSearchParams } from 'react-router-dom';
import {
  Bell,
  CalendarClock,
  Camera,
  Check,
  CreditCard,
  Edit,
  EyeOff,
  Plus,
  Save,
  ShoppingCart,
  Trash2,
  Upload,
  X,
} from 'lucide-react';

import Alert from '../components/ui/Alert.jsx';
import AuctionSummaryCard from '../components/marketplace/AuctionSummaryCard.jsx';
import Button from '../components/ui/Button.jsx';
import Card from '../components/ui/Card.jsx';
import CommentThread from '../components/marketplace/CommentThread.jsx';
import DataTable from '../components/ui/DataTable.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import FormField from '../components/ui/FormField.jsx';
import ListingSummaryCard from '../components/marketplace/ListingSummaryCard.jsx';
import LoadingState from '../components/ui/LoadingState.jsx';
import PageHeader from '../components/ui/PageHeader.jsx';
import PartSummaryCard from '../components/marketplace/PartSummaryCard.jsx';
import StatusBadge from '../components/ui/StatusBadge.jsx';
import VehicleImage from '../components/marketplace/VehicleImage.jsx';
import { adminApi, authApi, commentsApi, publicApi, storeApi, userApi } from '../services/reactApi.js';
import { dateOnly, dateTime, moneyMinor, moneyWhole } from '../utils/format.js';

export function HomePage() {
  const state = useLoad(publicApi.summary, []);
  if (state.loading) return <LoadingState label="Loading marketplace" />;
  if (state.error) return <Alert title="Marketplace unavailable">{state.error.message}</Alert>;
  const summary = state.data;

  return (
    <>
      <PageHeader
        eyebrow="React marketplace"
        title="Autostrada Auctions"
        description="Browse live auctions, fixed-price listings, and store inventory from the React frontend."
      />
      <div className="metric-grid">
        <Metric label="Live auctions" value={summary.featuredAuctions.length} />
        <Metric label="Fixed-price listings" value={summary.fixedPriceListings.length} tone="metric-indigo" />
        <Metric label="Store parts" value={summary.storeParts.length} tone="metric-amber" />
      </div>
      <SummarySection title="Featured auctions" to="/auctions">
        {summary.featuredAuctions.map((auction) => <AuctionSummaryCard key={auction.id} auction={auction} />)}
      </SummarySection>
      <SummarySection title="Fixed-price vehicles" to="/listings">
        {summary.fixedPriceListings.map((listing) => <ListingSummaryCard key={listing.id} listing={listing} />)}
      </SummarySection>
      <SummarySection title="Parts store" to="/parts" partGrid>
        {summary.storeParts.map((part) => <PartSummaryCard key={part.id} part={part} />)}
      </SummarySection>
    </>
  );
}

export function StaticContentPage({ page }) {
  const state = useLoad(() => publicApi.content(page), [page]);
  if (state.loading) return <LoadingState label="Loading page" />;
  if (state.error) return <Alert title="Page unavailable">{state.error.message}</Alert>;
  return (
    <>
      <PageHeader eyebrow="Autostrada Auctions" title={state.data.title} description={state.data.body} />
      <Card className="content-card">
        <p>{state.data.body}</p>
        <p>Use the marketplace navigation to browse vehicles, manage account activity, or review store orders.</p>
      </Card>
    </>
  );
}

export function LoginPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState(null);
  async function submit(event) {
    event.preventDefault();
    setError(null);
    try {
      await authApi.login(form);
      navigate('/');
      window.location.reload();
    } catch (err) {
      setError(err.message);
    }
  }
  return (
    <AuthPanel title="Sign in" description="Use your existing Autostrada Auctions account.">
      {error && <Alert title="Sign in failed">{error}</Alert>}
      <form className="form-grid" onSubmit={submit}>
        <FormField label="Username" name="username" value={form.username} onChange={bind(setForm)} />
        <FormField label="Password" name="password" type="password" value={form.password} onChange={bind(setForm)} />
        <Button type="submit">Sign in</Button>
      </form>
      <div className="inline-links">
        <Link to="/register">Create account</Link>
        <Link to="/forgot-password">Forgot password</Link>
      </div>
    </AuthPanel>
  );
}

export function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    address: '',
    streetAddress: '',
    city: '',
    postalCode: '',
    country: '',
    about: '',
  });
  const [error, setError] = useState(null);
  async function submit(event) {
    event.preventDefault();
    setError(null);
    try {
      await authApi.register(form);
      navigate('/register/thank-you');
    } catch (err) {
      setError(err.message);
    }
  }
  return (
    <>
      <PageHeader eyebrow="Account" title="Create account" description="One React form replaces the old two-step JSP registration flow." />
      {error && <Alert title="Registration failed">{error}</Alert>}
      <form className="form-grid two-col" onSubmit={submit}>
        {['username', 'email', 'password', 'firstName', 'lastName', 'phoneNumber', 'address', 'streetAddress', 'city', 'postalCode', 'country'].map((field) => (
          <FormField
            key={field}
            label={label(field)}
            name={field}
            type={field === 'password' ? 'password' : 'text'}
            value={form[field]}
            onChange={bind(setForm)}
          />
        ))}
        <FormField label="About" name="about" value={form.about} onChange={bind(setForm)} as="textarea" rows="4" />
        <div className="form-actions wide">
          <Button type="submit">Create account</Button>
        </div>
      </form>
    </>
  );
}

export function ThankYouPage() {
  return (
    <>
      <PageHeader eyebrow="Account created" title="You are ready to sign in" description="Your React account setup is complete." />
      <Button href="/login">Go to sign in</Button>
    </>
  );
}

export function ForgotPasswordPage() {
  const [identifier, setIdentifier] = useState('');
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  async function submit(event) {
    event.preventDefault();
    setError(null);
    setMessage(null);
    try {
      const result = await authApi.requestPasswordReset({ identifier });
      setMessage(result.message);
    } catch (err) {
      setError(err.message);
    }
  }
  return (
    <AuthPanel title="Reset password" description="Enter your username or email and we will send a reset link.">
      {message && <div className="success-panel">{message}</div>}
      {error && <Alert title="Request failed">{error}</Alert>}
      <form className="form-grid" onSubmit={submit}>
        <FormField label="Email or username" name="identifier" value={identifier} onChange={(event) => setIdentifier(event.target.value)} />
        <Button type="submit">Send reset link</Button>
      </form>
    </AuthPanel>
  );
}

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get('token') ?? '';
  const [form, setForm] = useState({ token, password: '', confirmPassword: '' });
  const [error, setError] = useState(null);
  async function submit(event) {
    event.preventDefault();
    setError(null);
    try {
      await authApi.completePasswordReset(form);
      navigate('/login');
    } catch (err) {
      setError(err.message);
    }
  }
  return (
    <AuthPanel title="Choose a new password" description="Complete the password reset from your emailed link.">
      {error && <Alert title="Reset failed">{error}</Alert>}
      <form className="form-grid" onSubmit={submit}>
        <FormField label="Token" name="token" value={form.token} onChange={bind(setForm)} />
        <FormField label="New password" name="password" type="password" value={form.password} onChange={bind(setForm)} />
        <FormField label="Confirm password" name="confirmPassword" type="password" value={form.confirmPassword} onChange={bind(setForm)} />
        <Button type="submit">Update password</Button>
      </form>
    </AuthPanel>
  );
}

export function AuctionDetailPage() {
  const params = useParams();
  const [searchParams] = useSearchParams();
  const id = params.id ?? searchParams.get('id') ?? searchParams.get('idCar');
  const { session } = useOutletContext();
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(() => publicApi.auction(id), [id, refresh]);
  if (state.loading) return <LoadingState label="Loading auction" />;
  if (state.error) return <Alert title="Auction unavailable">{state.error.message}</Alert>;
  const { auction, highestBid, following, comments } = state.data;
  return (
    <>
      <PageHeader
        eyebrow={auction.statusLabel}
        title={`${auction.make} ${auction.model}`}
        description={`${auction.year} auction managed from the React frontend.`}
      />
      <div className="detail-grid">
        <Card className="detail-media">
          <VehicleImage src={auction.imageUrl} alt={`${auction.make} ${auction.model}`} fallback={auction.make?.slice(0, 2)} />
        </Card>
        <Card className="detail-panel">
          <InfoList rows={[
            ['Asking price', moneyWhole(auction.price, 'EUR')],
            ['Highest bid', moneyWhole(highestBid, 'EUR')],
            ['Ends', auction.auctionEndTime],
            ['Seller', auction.sellerDisplayName],
          ]} />
          {session.authenticated && (
            <ActionStack>
              <BidForm auction={auction} onDone={() => setRefresh((value) => value + 1)} />
              <DateAction label="Request test drive" onSubmit={(date) => userApi.scheduleTestDrive(auction.id, date)} />
              <MutationButton icon={following ? EyeOff : Bell} run={() => following ? userApi.unfollowAuction(auction.id) : userApi.followAuction(auction.id)} onDone={() => setRefresh((value) => value + 1)}>
                {following ? 'Unfollow' : 'Follow'}
              </MutationButton>
            </ActionStack>
          )}
        </Card>
      </div>
      <CommentThread
        comments={comments}
        signedIn={session.authenticated}
        onSubmit={(payload) => commentsApi.addCarComment(auction.id, payload).then(() => setRefresh((value) => value + 1))}
      />
    </>
  );
}

export function ListingDetailPage() {
  const { id } = useParams();
  const { session } = useOutletContext();
  const state = useLoad(() => publicApi.listing(id), [id]);
  if (state.loading) return <LoadingState label="Loading listing" />;
  if (state.error) return <Alert title="Listing unavailable">{state.error.message}</Alert>;
  const { listing, description, stripeEnabled } = state.data;
  return (
    <>
      <PageHeader eyebrow={listing.status} title={listing.title} description={description} />
      <div className="detail-grid">
        <Card className="detail-media">
          <VehicleImage src={listing.imageUrl} alt={listing.title} fallback={listing.make?.slice(0, 2)} />
        </Card>
        <Card className="detail-panel">
          <InfoList rows={[
            ['Vehicle', `${listing.year} ${listing.make} ${listing.model}`],
            ['Mileage', `${listing.mileage.toLocaleString()} km`],
            ['Fuel', listing.fuelType],
            ['Transmission', listing.transmission],
            ['Price', moneyMinor(listing.priceMinor, 'EUR')],
            ['Deposit', moneyMinor(listing.depositAmountMinor, 'EUR')],
            ['Seller', listing.sellerDisplayName],
          ]} />
          {session.authenticated && (
            <ActionStack>
              <DateTimeAction label="Request test ride" onSubmit={(scheduledAt) => userApi.scheduleListingTestRide(listing.id, scheduledAt)} />
              <MutationButton icon={CreditCard} disabled={!stripeEnabled} run={() => userApi.listingDeposit(listing.id).then(openCheckout)}>
                Place deposit
              </MutationButton>
            </ActionStack>
          )}
        </Card>
      </div>
    </>
  );
}

export function PartDetailPage() {
  const { id } = useParams();
  const { session } = useOutletContext();
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(() => publicApi.part(id), [id, refresh]);
  if (state.loading) return <LoadingState label="Loading part" />;
  if (state.error) return <Alert title="Part unavailable">{state.error.message}</Alert>;
  const { part, comments } = state.data;
  return (
    <>
      <PageHeader eyebrow={part.category} title={part.name} description={part.description} />
      <div className="detail-grid">
        <Card className="part-detail-art">
          {part.imageUrl ? <img src={part.imageUrl} alt="" /> : <ShoppingCart size={72} aria-hidden="true" />}
        </Card>
        <Card className="detail-panel">
          <InfoList rows={[
            ['SKU', part.sku],
            ['Price', moneyMinor(part.priceMinor, 'EUR')],
            ['Stock', part.stockQuantity],
          ]} />
          {session.authenticated && (
            <MutationButton icon={ShoppingCart} run={() => storeApi.addToCart(part.id, 1)}>
              Add to cart
            </MutationButton>
          )}
        </Card>
      </div>
      <CommentThread
        comments={comments}
        signedIn={session.authenticated}
        onSubmit={(payload) => commentsApi.addPartComment(part.id, payload).then(() => setRefresh((value) => value + 1))}
      />
    </>
  );
}

export function PublicProfilePage() {
  const { idProfile } = useParams();
  const profileState = useLoad(() => publicApi.profile(idProfile), [idProfile]);
  const auctionsState = useLoad(() => publicApi.profileAuctions(idProfile), [idProfile]);
  if (profileState.loading || auctionsState.loading) return <LoadingState label="Loading seller profile" />;
  if (profileState.error) return <Alert title="Profile unavailable">{profileState.error.message}</Alert>;
  const profile = profileState.data;
  const auctions = auctionsState.data ?? [];
  return (
    <>
      <PageHeader eyebrow={profile.displayLocation || 'Seller'} title={`${profile.firstName} ${profile.lastName}`} description={profile.about || 'Autostrada Auctions seller profile.'} />
      <div className="summary-grid">
        {auctions.map((auction) => <AuctionSummaryCard key={auction.id} auction={auction} />)}
      </div>
      {auctions.length === 0 && <EmptyState title="No active auctions" description="This seller does not have active auctions right now." />}
    </>
  );
}

export function CartPage() {
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(storeApi.cart, [refresh]);
  if (state.loading) return <LoadingState label="Loading cart" />;
  if (state.error) return <Alert title="Cart unavailable">{state.error.message}</Alert>;
  const cart = state.data;
  return (
    <>
      <PageHeader eyebrow="Store" title="Cart" description="Review parts and start Stripe Checkout from React." />
      <DataTable
        rows={cart.items}
        columns={[
          { key: 'part', header: 'Part', render: (row) => row.part.name },
          { key: 'quantity', header: 'Qty' },
          { key: 'lineTotalMinor', header: 'Total', render: (row) => moneyMinor(row.lineTotalMinor, 'EUR') },
          { key: 'actions', header: '', render: (row) => <MutationButton variant="ghost" icon={Trash2} run={() => storeApi.removeCartItem(row.idCartItem)} onDone={() => setRefresh((value) => value + 1)}>Remove</MutationButton> },
        ]}
        emptyText="Your cart is empty."
      />
      <Card className="checkout-panel">
        <strong>Total: {moneyMinor(cart.totalMinor, 'EUR')}</strong>
        {!cart.hasShippingAddress && <Alert title="Shipping address needed">Complete your profile shipping address before checkout.</Alert>}
        <MutationButton icon={CreditCard} disabled={!cart.items.length || !cart.hasShippingAddress} run={() => storeApi.checkout().then(openCheckout)}>
          Checkout
        </MutationButton>
      </Card>
    </>
  );
}

export function OrdersPage() {
  return <PagedTable title="Orders" eyebrow="Store" load={(page) => storeApi.orders(page)} columns={orderColumns(false)} />;
}

export function OrderDetailPage({ admin = false }) {
  const { id } = useParams();
  const state = useLoad(() => admin ? adminApi.storeOrder(id) : storeApi.order(id), [id, admin]);
  if (state.loading) return <LoadingState label="Loading order" />;
  if (state.error) return <Alert title="Order unavailable">{state.error.message}</Alert>;
  const order = state.data;
  return (
    <>
      <PageHeader eyebrow={order.status} title={`Order #${order.idOrder}`} description={order.shippingAddress} />
      <Card className="detail-panel">
        <InfoList rows={[
          ['Customer', order.user?.username],
          ['Total', moneyMinor(order.totalMinor, order.currency)],
          ['Created', dateTime(order.createdAt)],
          ['Paid', order.paidAt ? dateTime(order.paidAt) : 'Pending'],
        ]} />
      </Card>
      <DataTable
        rows={order.items}
        columns={[
          { key: 'partName', header: 'Part' },
          { key: 'sku', header: 'SKU' },
          { key: 'quantity', header: 'Qty' },
          { key: 'unitPriceMinor', header: 'Unit', render: (row) => moneyMinor(row.unitPriceMinor, order.currency) },
          { key: 'lineTotalMinor', header: 'Total', render: (row) => moneyMinor(row.lineTotalMinor, order.currency) },
        ]}
      />
    </>
  );
}

export function CheckoutSuccessPage({ deposit = false }) {
  const [params] = useSearchParams();
  const sessionId = params.get('session_id');
  const state = useLoad(
    () => deposit ? userApi.listingDepositSuccess(sessionId) : storeApi.checkoutSuccess(sessionId),
    [deposit, sessionId],
  );
  if (state.loading) return <LoadingState label="Confirming checkout" />;
  if (state.error) return <Alert title="Checkout lookup failed">{state.error.message}</Alert>;
  return (
    <>
      <PageHeader eyebrow="Payment" title="Checkout complete" description="Stripe returned a successful checkout session." />
      <Card className="detail-panel">
        <pre className="json-preview">{JSON.stringify(state.data, null, 2)}</pre>
      </Card>
    </>
  );
}

export function ProfilePage() {
  const state = useLoad(userApi.profile, []);
  if (state.loading) return <LoadingState label="Loading profile" />;
  if (state.error) return <Alert title="Profile unavailable">{state.error.message}</Alert>;
  const profile = state.data;
  return (
    <>
      <PageHeader eyebrow="Account" title={`${profile.firstName} ${profile.lastName}`} description={profile.about || 'Manage your marketplace identity.'} actions={<Button href="/user/profile/edit" icon={Edit}>Edit</Button>} />
      <Card className="profile-card">
        {profile.pictureDataUrl && <img src={profile.pictureDataUrl} alt="" />}
        <InfoList rows={[
          ['Email', profile.email],
          ['Phone', profile.phoneNumber],
          ['Location', profile.displayLocation],
          ['Shipping', profile.formattedShippingAddress || 'Not completed'],
        ]} />
      </Card>
      <UploadPanel label="Update profile picture" onUpload={userApi.updateProfilePicture} />
    </>
  );
}

export function ProfileEditPage() {
  return <ProfileForm load={userApi.profile} save={userApi.updateProfile} title="Edit profile" />;
}

export function UserAuctionsPage() {
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(userApi.auctions, [refresh]);
  if (state.loading) return <LoadingState label="Loading auctions" />;
  if (state.error) return <Alert title="Auctions unavailable">{state.error.message}</Alert>;
  return (
    <>
      <PageHeader eyebrow="Account" title="My auctions" description="Manage auction posts and pictures." actions={<Button href="/user/auctions/new" icon={Plus}>New auction</Button>} />
      <div className="summary-grid">
        {state.data.map((auction) => (
          <Card className="summary-card" key={auction.id}>
            <VehicleImage src={auction.imageUrl} alt={`${auction.make} ${auction.model}`} fallback={auction.make?.slice(0, 2)} />
            <div className="summary-card-body">
              <h2>{auction.make} {auction.model}</h2>
              <p>{moneyWhole(auction.price, 'EUR')}</p>
              <StatusBadge value={auction.status} />
            </div>
            <div className="card-actions">
              <Button href={`/user/auctions/${auction.id}/edit`} variant="secondary" icon={Edit}>Edit</Button>
              <MutationButton variant="ghost" icon={Check} run={() => userApi.activateAuction(auction.id)} onDone={() => setRefresh((value) => value + 1)}>Activate</MutationButton>
              <MutationButton variant="ghost" icon={EyeOff} run={() => userApi.deactivateAuction(auction.id)} onDone={() => setRefresh((value) => value + 1)}>Hide</MutationButton>
            </div>
          </Card>
        ))}
      </div>
    </>
  );
}

export function AuctionFormPage() {
  const params = useParams();
  const [searchParams] = useSearchParams();
  const id = params.id ?? searchParams.get('id') ?? searchParams.get('idCar');
  const editing = Boolean(id);
  const navigate = useNavigate();
  const [form, setForm] = useState({ make: '', model: '', year: '', price: '', auctionEndTime: '' });
  const [file, setFile] = useState(null);
  const [error, setError] = useState(null);
  useEffect(() => {
    if (editing) {
      userApi.auction(id).then((auction) => {
        setForm({
          make: auction.make,
          model: auction.model,
          year: auction.year,
          price: auction.price,
          auctionEndTime: '',
        });
      }).catch((err) => setError(err.message));
    }
  }, [editing, id]);
  async function submit(event) {
    event.preventDefault();
    setError(null);
    try {
      if (editing) {
        await userApi.updateAuction(id, { ...form, price: Number(form.price) });
      } else {
        await userApi.createAuction({ ...form, price: Number(form.price) }, file);
      }
      navigate('/user/auctions');
    } catch (err) {
      setError(err.message);
    }
  }
  return (
    <EntityForm title={editing ? 'Edit auction' : 'New auction'} error={error} onSubmit={submit}>
      {['make', 'model', 'year', 'price'].map((field) => (
        <FormField key={field} label={label(field)} name={field} value={form[field]} onChange={bind(setForm)} />
      ))}
      <FormField label="Auction end" name="auctionEndTime" type="datetime-local" value={form.auctionEndTime} onChange={bind(setForm)} />
      {!editing && <input type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />}
    </EntityForm>
  );
}

export function UserListingsPage() {
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(userApi.listings, [refresh]);
  if (state.loading) return <LoadingState label="Loading listings" />;
  if (state.error) return <Alert title="Listings unavailable">{state.error.message}</Alert>;
  return (
    <>
      <PageHeader eyebrow="Account" title="My listings" description="Manage fixed-price car listings." actions={<Button href="/user/listings/new" icon={Plus}>New listing</Button>} />
      <DataTable
        rows={state.data}
        columns={[
          { key: 'title', header: 'Title' },
          { key: 'vehicle', header: 'Vehicle', render: (row) => `${row.year} ${row.make} ${row.model}` },
          { key: 'priceMinor', header: 'Price', render: (row) => moneyMinor(row.priceMinor, 'EUR') },
          { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'actions', header: '', render: (row) => (
            <div className="table-actions">
              <Button href={`/user/listings/${row.id}/edit`} variant="secondary" icon={Edit}>Edit</Button>
              <MutationButton variant="ghost" icon={Check} run={() => userApi.activateListing(row.id)} onDone={() => setRefresh((value) => value + 1)}>Activate</MutationButton>
              <MutationButton variant="ghost" icon={EyeOff} run={() => userApi.deactivateListing(row.id)} onDone={() => setRefresh((value) => value + 1)}>Hide</MutationButton>
            </div>
          ) },
        ]}
      />
    </>
  );
}

export function ListingFormPage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: '', make: '', model: '', year: '', mileage: '', fuelType: '', transmission: '', price: '', depositAmount: '', description: '' });
  const [file, setFile] = useState(null);
  const [error, setError] = useState(null);
  useEffect(() => {
    if (editing) {
      userApi.listingForm(id).then((data) => setForm({ ...data, price: String(data.price), depositAmount: String(data.depositAmount) })).catch((err) => setError(err.message));
    }
  }, [editing, id]);
  async function submit(event) {
    event.preventDefault();
    setError(null);
    try {
      if (editing) {
        await userApi.updateListing(id, form, file);
      } else {
        await userApi.createListing(form, file);
      }
      navigate('/user/listings');
    } catch (err) {
      setError(err.message);
    }
  }
  return (
    <EntityForm title={editing ? 'Edit listing' : 'New listing'} error={error} onSubmit={submit}>
      {['title', 'make', 'model', 'year', 'mileage', 'fuelType', 'transmission', 'price', 'depositAmount'].map((field) => (
        <FormField key={field} label={label(field)} name={field} value={form[field]} onChange={bind(setForm)} />
      ))}
      <FormField label="Description" name="description" as="textarea" rows="5" value={form.description} onChange={bind(setForm)} />
      <input type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
    </EntityForm>
  );
}

export function BidsPage() {
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(userApi.bids, [refresh]);
  return <SimpleState state={state} title="My bids" render={(rows) => (
    <DataTable rows={rows} columns={bidColumns((row) => <MutationButton variant="ghost" icon={X} run={() => userApi.cancelBid(row.idBid)} onDone={() => setRefresh((value) => value + 1)}>Cancel</MutationButton>)} />
  )} />;
}

export function WatchlistPage() {
  const state = useLoad(userApi.followedAuctions, []);
  return <SimpleState state={state} title="Watchlist" render={(rows) => <div className="summary-grid">{rows.map((auction) => <AuctionSummaryCard key={auction.id} auction={auction} />)}</div>} />;
}

export function NotificationsPage() {
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(userApi.notifications, [refresh]);
  return <SimpleState state={state} title="Notifications" render={(rows) => (
    <>
      <MutationButton icon={Check} run={userApi.markAllNotificationsRead} onDone={() => setRefresh((value) => value + 1)}>Mark all read</MutationButton>
      <DataTable
        rows={rows}
        columns={[
          { key: 'message', header: 'Message' },
          { key: 'createdAt', header: 'Created', render: (row) => dateTime(row.createdAt) },
          { key: 'read', header: 'State', render: (row) => row.read ? 'Read' : 'Unread' },
          { key: 'actions', header: '', render: (row) => !row.read && <MutationButton variant="ghost" icon={Check} run={() => userApi.markNotificationRead(row.idNotification)} onDone={() => setRefresh((value) => value + 1)}>Read</MutationButton> },
        ]}
      />
    </>
  )} />;
}

export function AppointmentsPage() {
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(userApi.appointments, [refresh]);
  if (state.loading) return <LoadingState label="Loading appointments" />;
  if (state.error) return <Alert title="Appointments unavailable">{state.error.message}</Alert>;
  const reload = () => setRefresh((value) => value + 1);
  return (
    <>
      <PageHeader eyebrow="Account" title="Appointments" description="Test drives and fixed-price test rides." />
      <AppointmentTable title="Booked auction test drives" rows={state.data.bookedTestDrives} reload={reload} />
      <AppointmentTable title="Received auction test-drive requests" rows={state.data.receivedTestDrives} owner reload={reload} />
      <ListingRideTable title="Booked listing test rides" rows={state.data.listingTestRides} reload={reload} />
      <ListingRideTable title="Received listing test-ride requests" rows={state.data.listingTestRideRequests} owner reload={reload} />
    </>
  );
}

export function DepositsPage() {
  return <PagedTable title="Listing deposits" eyebrow="Account" load={(page) => userApi.listingDeposits(page)} columns={[
    { key: 'listing', header: 'Listing', render: (row) => row.listing.title },
    { key: 'amountMinor', header: 'Amount', render: (row) => moneyMinor(row.amountMinor, row.currency) },
    { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
    { key: 'createdAt', header: 'Created', render: (row) => dateTime(row.createdAt) },
  ]} />;
}

export function AdminUsersPage() {
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(() => adminApi.dashboard(), [refresh]);
  if (state.loading) return <LoadingState label="Loading users" />;
  if (state.error) return <Alert title="Admin unavailable">{state.error.message}</Alert>;
  const columns = [
    { key: 'username', header: 'Username' },
    { key: 'email', header: 'Email' },
    { key: 'profile', header: 'Name', render: (row) => `${row.profile?.firstName ?? ''} ${row.profile?.lastName ?? ''}` },
    { key: 'actions', header: '', render: (row) => <MutationButton variant="ghost" icon={ShieldIcon} run={() => adminApi.markAdmin(row.idUser)} onDone={() => setRefresh((value) => value + 1)}>Make admin</MutationButton> },
  ];
  return (
    <>
      <PageHeader eyebrow="Admin" title="Users" description="Manage regular and admin accounts." />
      <SectionTitle title="Users" />
      <DataTable rows={state.data.users.content} columns={columns} />
      <SectionTitle title="Admins" />
      <DataTable rows={state.data.admins.content} columns={columns.slice(0, 3)} />
    </>
  );
}

export function AdminCarsPage() {
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(() => adminApi.cars(), [refresh]);
  if (state.loading) return <LoadingState label="Loading auction admin" />;
  if (state.error) return <Alert title="Admin unavailable">{state.error.message}</Alert>;
  return (
    <>
      <PageHeader eyebrow="Admin" title="Auction moderation" description="Review auction posts and bids." />
      <DataTable rows={state.data.cars.content} columns={[
        { key: 'make', header: 'Vehicle', render: (row) => `${row.year} ${row.make} ${row.model}` },
        { key: 'price', header: 'Price', render: (row) => moneyWhole(row.price, 'EUR') },
        { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'actions', header: '', render: (row) => <div className="table-actions"><MutationButton variant="ghost" icon={Check} run={() => adminApi.activateCar(row.id)} onDone={() => setRefresh((value) => value + 1)}>Activate</MutationButton><MutationButton variant="ghost" icon={EyeOff} run={() => adminApi.deactivateCar(row.id)} onDone={() => setRefresh((value) => value + 1)}>Hide</MutationButton></div> },
      ]} />
      <SectionTitle title="Bids" />
      <DataTable rows={state.data.bids.content} columns={bidColumns((row) => <div className="table-actions"><MutationButton variant="ghost" icon={Check} run={() => adminApi.approveBid(row.idBid)} onDone={() => setRefresh((value) => value + 1)}>Approve</MutationButton><MutationButton variant="ghost" icon={X} run={() => adminApi.denyBid(row.idBid)} onDone={() => setRefresh((value) => value + 1)}>Deny</MutationButton></div>)} />
    </>
  );
}

export function AdminTransactionsPage() {
  const state = useLoad(() => adminApi.transactions(), []);
  if (state.loading) return <LoadingState label="Loading transactions" />;
  if (state.error) return <Alert title="Transactions unavailable">{state.error.message}</Alert>;
  return (
    <>
      <PageHeader eyebrow="Admin" title="Transactions" description="Payment orders and processed webhook events." />
      <DataTable rows={state.data.transactions.content} columns={[
        { key: 'idPayment', header: 'ID' },
        { key: 'amountMinor', header: 'Amount', render: (row) => moneyMinor(row.amountMinor, row.currency) },
        { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'buyer', header: 'Buyer', render: (row) => row.buyer?.username },
        { key: 'seller', header: 'Seller', render: (row) => row.seller?.username },
      ]} />
      <SectionTitle title="Webhook events" />
      <DataTable rows={state.data.webhookEvents} columns={[
        { key: 'stripeEventId', header: 'Event' },
        { key: 'eventType', header: 'Type' },
        { key: 'processedAt', header: 'Processed', render: (row) => dateTime(row.processedAt) },
      ]} />
    </>
  );
}

export function AdminStorePartsPage() {
  const [refresh, setRefresh] = useState(0);
  const state = useLoad(() => adminApi.storeParts(), [refresh]);
  if (state.loading) return <LoadingState label="Loading store parts" />;
  if (state.error) return <Alert title="Store admin unavailable">{state.error.message}</Alert>;
  return (
    <>
      <PageHeader eyebrow="Admin" title="Store parts" description="Manage parts inventory." actions={<Button href="/admin/store/parts/new" icon={Plus}>New part</Button>} />
      <DataTable rows={state.data.content} columns={[
        { key: 'sku', header: 'SKU' },
        { key: 'name', header: 'Name' },
        { key: 'category', header: 'Category' },
        { key: 'priceMinor', header: 'Price', render: (row) => moneyMinor(row.priceMinor, 'EUR') },
        { key: 'stockQuantity', header: 'Stock' },
        { key: 'actions', header: '', render: (row) => <div className="table-actions"><Button href={`/admin/store/parts/${row.id}/edit`} variant="secondary" icon={Edit}>Edit</Button><MutationButton variant="ghost" icon={EyeOff} run={() => adminApi.setStorePartActive(row.id, false)} onDone={() => setRefresh((value) => value + 1)}>Hide</MutationButton></div> },
      ]} />
    </>
  );
}

export function AdminPartFormPage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const [form, setForm] = useState({ sku: '', name: '', category: '', description: '', priceMinor: '', stockQuantity: '', imageUrl: '', active: true });
  const [error, setError] = useState(null);
  useEffect(() => {
    if (editing) {
      adminApi.storePart(id).then(setForm).catch((err) => setError(err.message));
    }
  }, [editing, id]);
  async function submit(event) {
    event.preventDefault();
    setError(null);
    try {
      const body = { ...form, priceMinor: Number(form.priceMinor), stockQuantity: Number(form.stockQuantity), active: Boolean(form.active) };
      if (editing) {
        await adminApi.updateStorePart(id, body);
      } else {
        await adminApi.createStorePart(body);
      }
      navigate('/admin/store/parts');
    } catch (err) {
      setError(err.message);
    }
  }
  return (
    <EntityForm title={editing ? 'Edit part' : 'New part'} error={error} onSubmit={submit}>
      {['sku', 'name', 'category', 'priceMinor', 'stockQuantity', 'imageUrl'].map((field) => (
        <FormField key={field} label={label(field)} name={field} value={form[field]} onChange={bind(setForm)} />
      ))}
      <FormField label="Description" name="description" as="textarea" rows="5" value={form.description} onChange={bind(setForm)} />
    </EntityForm>
  );
}

export function AdminOrdersPage() {
  return <PagedTable title="Store orders" eyebrow="Admin" load={(page) => adminApi.storeOrders({ page })} columns={orderColumns(true)} />;
}

function Metric({ label, value, tone = '' }) {
  return <Card className={`metric-card ${tone}`}><span>{label}</span><strong>{value}</strong></Card>;
}

function SummarySection({ title, to, partGrid = false, children }) {
  return (
    <section>
      <div className="section-header">
        <h2>{title}</h2>
        <Link to={to}>View all</Link>
      </div>
      <div className={partGrid ? 'part-grid' : 'summary-grid'}>{children}</div>
    </section>
  );
}

function AuthPanel({ title, description, children }) {
  return (
    <>
      <PageHeader eyebrow="Account" title={title} description={description} />
      <Card className="auth-card">{children}</Card>
    </>
  );
}

function InfoList({ rows }) {
  return <dl className="info-list">{rows.map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{value || 'Not set'}</dd></div>)}</dl>;
}

function ActionStack({ children }) {
  return <div className="action-stack">{children}</div>;
}

function BidForm({ auction, onDone }) {
  const [bidPrice, setBidPrice] = useState('');
  return (
    <form className="inline-form" onSubmit={(event) => {
      event.preventDefault();
      userApi.bid(auction.id, Number(bidPrice)).then(onDone);
    }}>
      <input value={bidPrice} onChange={(event) => setBidPrice(event.target.value)} placeholder="Bid amount" />
      <Button type="submit" icon={CreditCard}>Bid</Button>
    </form>
  );
}

function DateAction({ label, onSubmit }) {
  const [date, setDate] = useState('');
  return (
    <form className="inline-form" onSubmit={(event) => {
      event.preventDefault();
      onSubmit(date);
    }}>
      <input type="date" value={date} onChange={(event) => setDate(event.target.value)} />
      <Button type="submit" icon={CalendarClock}>{label}</Button>
    </form>
  );
}

function DateTimeAction({ label, onSubmit }) {
  const [value, setValue] = useState('');
  return (
    <form className="inline-form" onSubmit={(event) => {
      event.preventDefault();
      onSubmit(value);
    }}>
      <input type="datetime-local" value={value} onChange={(event) => setValue(event.target.value)} />
      <Button type="submit" icon={CalendarClock}>{label}</Button>
    </form>
  );
}

function MutationButton({ run, onDone, children, icon, variant = 'secondary', disabled = false }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  async function click() {
    setBusy(true);
    setError(null);
    try {
      await run();
      onDone?.();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <span className="mutation-wrap">
      <Button icon={icon} variant={variant} onClick={click} disabled={busy || disabled}>{busy ? 'Working' : children}</Button>
      {error && <small className="field-error">{error}</small>}
    </span>
  );
}

function ProfileForm({ load, save, title }) {
  const navigate = useNavigate();
  const state = useLoad(load, []);
  const [form, setForm] = useState(null);
  const [error, setError] = useState(null);
  useEffect(() => {
    if (state.data) setForm(state.data);
  }, [state.data]);
  if (state.loading || !form) return <LoadingState label="Loading profile form" />;
  if (state.error) return <Alert title="Profile unavailable">{state.error.message}</Alert>;
  async function submit(event) {
    event.preventDefault();
    setError(null);
    try {
      await save(form);
      navigate('/user/profile');
    } catch (err) {
      setError(err.message);
    }
  }
  return (
    <EntityForm title={title} error={error} onSubmit={submit}>
      {['email', 'firstName', 'lastName', 'phoneNumber', 'address', 'streetAddress', 'city', 'postalCode', 'country'].map((field) => (
        <FormField key={field} label={label(field)} name={field} value={form[field]} onChange={bind(setForm)} />
      ))}
      <FormField label="About" name="about" as="textarea" rows="4" value={form.about} onChange={bind(setForm)} />
    </EntityForm>
  );
}

function UploadPanel({ label: text, onUpload }) {
  const [file, setFile] = useState(null);
  return (
    <Card className="upload-panel">
      <strong>{text}</strong>
      <input type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
      <MutationButton icon={Upload} disabled={!file} run={() => onUpload(file).then(() => window.location.reload())}>Upload</MutationButton>
    </Card>
  );
}

function EntityForm({ title, error, onSubmit, children }) {
  return (
    <>
      <PageHeader eyebrow="Form" title={title} description="Validated by the Spring Boot API." />
      {error && <Alert title="Save failed">{error}</Alert>}
      <form className="form-grid two-col" onSubmit={onSubmit}>
        {children}
        <div className="form-actions wide">
          <Button type="submit" icon={Save}>Save</Button>
        </div>
      </form>
    </>
  );
}

function PagedTable({ title, eyebrow, load, columns }) {
  const [page, setPage] = useState(0);
  const state = useLoad(() => load(page), [page]);
  if (state.loading) return <LoadingState label={`Loading ${title.toLowerCase()}`} />;
  if (state.error) return <Alert title={`${title} unavailable`}>{state.error.message}</Alert>;
  return (
    <>
      <PageHeader eyebrow={eyebrow} title={title} description="React table backed by the Spring Boot API." />
      <DataTable rows={state.data.content} columns={columns} />
      <div className="pager">
        <Button variant="secondary" disabled={page <= 0} onClick={() => setPage((value) => value - 1)}>Previous</Button>
        <span>Page {state.data.page + 1} of {state.data.totalPages || 1}</span>
        <Button variant="secondary" disabled={state.data.last} onClick={() => setPage((value) => value + 1)}>Next</Button>
      </div>
    </>
  );
}

function SimpleState({ state, title, render }) {
  if (state.loading) return <LoadingState label={`Loading ${title.toLowerCase()}`} />;
  if (state.error) return <Alert title={`${title} unavailable`}>{state.error.message}</Alert>;
  return (
    <>
      <PageHeader eyebrow="Account" title={title} description="Migrated from JSP to React." />
      {render(state.data)}
    </>
  );
}

function AppointmentTable({ title, rows, owner = false, reload }) {
  return (
    <>
      <SectionTitle title={title} />
      <DataTable rows={rows} columns={[
        { key: 'auction', header: 'Auction', render: (row) => `${row.auction.year} ${row.auction.make} ${row.auction.model}` },
        { key: 'date', header: 'Date', render: (row) => dateOnly(row.date) },
        { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'actions', header: '', render: (row) => owner ? ownerDriveActions(row.idTestDrive, reload) : userDriveActions(row.idTestDrive, reload) },
      ]} />
    </>
  );
}

function ListingRideTable({ title, rows, owner = false, reload }) {
  return (
    <>
      <SectionTitle title={title} />
      <DataTable rows={rows} columns={[
        { key: 'listing', header: 'Listing', render: (row) => row.listing.title },
        { key: 'scheduledAt', header: 'Date', render: (row) => dateTime(row.scheduledAt) },
        { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'actions', header: '', render: (row) => owner ? ownerListingRideActions(row.idTestRide, reload) : userListingRideActions(row.idTestRide, reload) },
      ]} />
    </>
  );
}

function ownerDriveActions(id, reload) {
  return <div className="table-actions"><MutationButton variant="ghost" icon={Check} run={() => userApi.acceptTestDrive(id)} onDone={reload}>Accept</MutationButton><MutationButton variant="ghost" icon={X} run={() => userApi.rejectTestDrive(id)} onDone={reload}>Reject</MutationButton></div>;
}

function userDriveActions(id, reload) {
  return <MutationButton variant="ghost" icon={X} run={() => userApi.cancelTestDrive(id)} onDone={reload}>Cancel</MutationButton>;
}

function ownerListingRideActions(id, reload) {
  return <div className="table-actions"><MutationButton variant="ghost" icon={Check} run={() => userApi.acceptListingTestRide(id)} onDone={reload}>Accept</MutationButton><MutationButton variant="ghost" icon={X} run={() => userApi.rejectListingTestRide(id)} onDone={reload}>Reject</MutationButton></div>;
}

function userListingRideActions(id, reload) {
  return <MutationButton variant="ghost" icon={X} run={() => userApi.cancelListingTestRide(id)} onDone={reload}>Cancel</MutationButton>;
}

function SectionTitle({ title }) {
  return <div className="section-header"><h2>{title}</h2></div>;
}

function orderColumns(admin) {
  return [
    { key: 'idOrder', header: 'Order', render: (row) => <Link to={admin ? `/admin/store/orders/${row.idOrder}` : `/orders/${row.idOrder}`}>#{row.idOrder}</Link> },
    { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
    { key: 'totalMinor', header: 'Total', render: (row) => moneyMinor(row.totalMinor, row.currency) },
    { key: 'createdAt', header: 'Created', render: (row) => dateTime(row.createdAt) },
    { key: 'user', header: 'Customer', render: (row) => row.user?.username },
  ];
}

function bidColumns(actions) {
  return [
    { key: 'auction', header: 'Auction', render: (row) => `${row.auction.year} ${row.auction.make} ${row.auction.model}` },
    { key: 'bidPrice', header: 'Bid', render: (row) => moneyWhole(row.bidPrice, 'EUR') },
    { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
    { key: 'actions', header: '', render: actions },
  ];
}

function useLoad(load, deps) {
  const [state, setState] = useState({ data: null, error: null, loading: true });
  useEffect(() => {
    let active = true;
    setState((current) => ({ ...current, loading: true, error: null }));
    load()
      .then((data) => active && setState({ data, error: null, loading: false }))
      .catch((error) => active && setState({ data: null, error, loading: false }));
    return () => {
      active = false;
    };
  }, deps);
  return state;
}

function bind(setForm) {
  return (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };
}

function label(value) {
  return value
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (match) => match.toUpperCase());
}

function openCheckout(result) {
  if (result.checkoutUrl) {
    window.location.href = result.checkoutUrl;
  }
}

function ShieldIcon(props) {
  return <Check {...props} />;
}
