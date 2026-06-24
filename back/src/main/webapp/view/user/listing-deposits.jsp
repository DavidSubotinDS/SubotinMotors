<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/user.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main class="account-main">
      <div class="container py-4 py-lg-5">
        <div class="account-shell">
          <c:set var="accountNavActive" value="deposits" />
          <%@ include file="../components/user-sidebar.jsp" %>
          <div class="content-wrapper account-content-surface">
            <h1 class="h2 fw-bold">My reservation deposits</h1>
            <p class="text-secondary">Deposits reserve a listing; they are not the full vehicle purchase.</p>
            <div class="table-responsive">
              <table class="table align-middle">
                <thead><tr><th>Listing</th><th>Amount</th><th>Status</th><th>PSP reference</th><th>Created</th></tr></thead>
                <tbody>
                  <c:forEach items="${deposits}" var="deposit">
                    <tr>
                      <td><a href="${pageContext.request.contextPath}/listings/${deposit.listing.idListing}">${deposit.listing.title}</a></td>
                      <td><fmt:formatNumber value="${deposit.amount}" minFractionDigits="2" maxFractionDigits="2" /> ${deposit.currency.toUpperCase()}</td>
                      <td>${deposit.status}</td>
                      <td><c:choose><c:when test="${not empty deposit.paymentIntentId}">${deposit.paymentIntentId}</c:when><c:otherwise>${deposit.checkoutSessionId}</c:otherwise></c:choose></td>
                      <td>${deposit.createdAt}</td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty deposits}"><tr><td colspan="5" class="text-secondary">You have no listing deposits.</td></tr></c:if>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
