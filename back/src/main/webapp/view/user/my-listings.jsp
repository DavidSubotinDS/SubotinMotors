<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/user.css" />
    <link rel="stylesheet" href="/css/listings.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main class="account-main">
      <div class="container py-4 py-lg-5">
        <div class="account-shell">
          <c:set var="accountNavActive" value="fixedListings" />
          <%@ include file="../components/user-sidebar.jsp" %>
          <div class="content-wrapper account-content-surface">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <div>
                <h1 class="h2 fw-bold mb-1">My fixed-price listings</h1>
                <p class="text-secondary mb-0">Manage cars offered outside the auction flow.</p>
              </div>
              <a class="btn btn-primary" href="${pageContext.request.contextPath}/user/listings/new">New listing</a>
            </div>
            <c:if test="${not empty listingMessage}"><div class="alert alert-success">${listingMessage}</div></c:if>
            <div class="table-responsive">
              <table class="table align-middle">
                <thead><tr><th>Car</th><th>Price</th><th>Deposit</th><th>Status</th><th>Actions</th></tr></thead>
                <tbody>
                  <c:forEach items="${listings}" var="listing">
                    <tr>
                      <td><a href="${pageContext.request.contextPath}/listings/${listing.idListing}">${listing.title}</a><br /><span class="small text-secondary">${listing.make} ${listing.model} ${listing.year}</span></td>
                      <td><fmt:formatNumber value="${listing.priceAmount}" minFractionDigits="2" maxFractionDigits="2" /> EUR</td>
                      <td><fmt:formatNumber value="${listing.depositAmount}" minFractionDigits="2" maxFractionDigits="2" /> EUR</td>
                      <td><span class="badge ${listing.active ? 'bg-success' : listing.reserved ? 'bg-warning text-dark' : 'bg-secondary'}">${listing.status.label}</span></td>
                      <td>
                        <div class="d-flex flex-wrap gap-2">
                          <c:if test="${listing.status eq 'ACTIVE' or listing.status eq 'INACTIVE'}">
                            <a class="btn btn-sm btn-outline-primary" href="${pageContext.request.contextPath}/user/listings/${listing.idListing}/edit">Edit</a>
                          </c:if>
                          <c:if test="${listing.active}">
                            <form:form action="${pageContext.request.contextPath}/user/listings/${listing.idListing}/deactivate" method="POST"><button class="btn btn-sm btn-outline-danger" type="submit">Deactivate</button></form:form>
                          </c:if>
                          <c:if test="${listing.status eq 'INACTIVE'}">
                            <form:form action="${pageContext.request.contextPath}/user/listings/${listing.idListing}/activate" method="POST"><button class="btn btn-sm btn-success" type="submit">Activate</button></form:form>
                          </c:if>
                        </div>
                      </td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty listings}"><tr><td colspan="5" class="text-secondary">You have not created a fixed-price listing yet.</td></tr></c:if>
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
