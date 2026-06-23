<%@ include file="components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head><%@ include file="components/header.jsp" %></head>
  <body>
    <%@ include file="components/navbar.jsp" %>
    <main class="container py-5">
      <div class="mx-auto border rounded-4 p-4 p-md-5" style="max-width: 720px">
        <span class="badge bg-success mb-3">Reservation submitted</span>
        <h1 class="fw-bold">${deposit.listing.title}</h1>
        <p>Your <fmt:formatNumber value="${deposit.amount}" minFractionDigits="2" maxFractionDigits="2" /> ${deposit.currency.toUpperCase()} deposit is recorded with status <strong>${deposit.status}</strong>.</p>
        <p class="text-secondary">The signed Stripe webhook is the final source of truth for payment completion. This deposit reserves the listing; it does not complete the full car sale.</p>
        <a class="btn btn-primary me-2" href="${pageContext.request.contextPath}/user/listing-deposits">View my deposits</a>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/listings/${deposit.listing.idListing}">Back to listing</a>
      </div>
    </main>
    <%@ include file="components/footer.jsp" %>
  </body>
</html>
