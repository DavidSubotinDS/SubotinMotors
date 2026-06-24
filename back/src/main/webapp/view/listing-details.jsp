<%@ include file="components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="components/header.jsp" %>
    <link rel="stylesheet" href="/css/listings.css" />
  </head>
  <body>
    <%@ include file="components/navbar.jsp" %>

    <main class="container py-5">
      <c:if test="${not empty listingMessage}"><div class="alert alert-success">${listingMessage}</div></c:if>
      <c:if test="${not empty listingError}"><div class="alert alert-danger">${listingError}</div></c:if>
      <c:if test="${param.depositCanceled != null}"><div class="alert alert-warning">Deposit checkout was cancelled. The reservation will be released when the PSP session expires.</div></c:if>

      <div class="row g-4">
        <div class="col-lg-8">
          <div class="listing-detail-image">
            <c:choose>
              <c:when test="${not empty listing.picture}">
                <img src="data:${listing.picture.fileType};base64,${listing.picture.image}" alt="${listing.title}" />
              </c:when>
              <c:otherwise>
                <div class="listing-placeholder"><i class="fa-solid fa-car"></i></div>
              </c:otherwise>
            </c:choose>
          </div>
          <section class="listing-panel mt-4">
            <h2 class="h4 fw-bold">About this car</h2>
            <p class="mb-0 listing-description">${listing.description}</p>
          </section>
        </div>

        <div class="col-lg-4">
          <aside class="listing-panel listing-actions">
            <div class="d-flex justify-content-between gap-2 align-items-start">
              <div>
                <p class="text-secondary mb-1">${listing.make} ${listing.model} &middot; ${listing.year}</p>
                <h1 class="h3 fw-bold">${listing.title}</h1>
              </div>
              <span class="badge ${listing.active ? 'bg-success' : listing.reserved ? 'bg-warning text-dark' : 'bg-secondary'}">${listing.status.label}</span>
            </div>
            <p class="listing-price"><fmt:formatNumber value="${listing.priceAmount}" minFractionDigits="2" maxFractionDigits="2" /> EUR</p>
            <dl class="row small">
              <dt class="col-5">Mileage</dt><dd class="col-7"><fmt:formatNumber value="${listing.mileage}" /> km</dd>
              <dt class="col-5">Fuel</dt><dd class="col-7">${listing.fuelType}</dd>
              <dt class="col-5">Transmission</dt><dd class="col-7">${listing.transmission}</dd>
              <dt class="col-5">Seller</dt><dd class="col-7">${listing.seller.profile.firstName} ${listing.seller.profile.lastName}</dd>
            </dl>

            <security:authorize access="isAuthenticated()">
              <c:if test="${listing.active}">
                <hr />
                <h2 class="h5 fw-bold">Schedule a test ride</h2>
                <form:form action="${pageContext.request.contextPath}/listings/${listing.idListing}/test-rides" method="POST" modelAttribute="testRide">
                  <form:errors path="scheduledAt" cssClass="error d-block mb-2" />
                  <form:input class="form-control mb-2" type="datetime-local" path="scheduledAt" />
                  <button class="btn btn-outline-primary w-100" type="submit">Request test ride</button>
                </form:form>

                <hr />
                <h2 class="h5 fw-bold">Reserve with a deposit</h2>
                <p class="small text-secondary">This is a reservation payment, not the full vehicle purchase price.</p>
                <p class="fw-bold"><fmt:formatNumber value="${listing.depositAmount}" minFractionDigits="2" maxFractionDigits="2" /> EUR deposit</p>
                <c:choose>
                  <c:when test="${stripeEnabled}">
                    <form:form action="${pageContext.request.contextPath}/user/listings/${listing.idListing}/deposit" method="POST">
                      <button class="btn btn-success w-100" type="submit"><i class="fa-brands fa-stripe me-2"></i>Reserve this car</button>
                    </form:form>
                  </c:when>
                  <c:otherwise>
                    <div class="alert alert-warning small mb-0">Stripe sandbox checkout is not enabled.</div>
                  </c:otherwise>
                </c:choose>
              </c:if>
              <c:if test="${listing.seller.username eq pageContext.request.userPrincipal.name}">
                <a class="btn btn-outline-secondary w-100 mt-3" href="${pageContext.request.contextPath}/user/listings/${listing.idListing}/edit">Edit my listing</a>
              </c:if>
            </security:authorize>
            <security:authorize access="!isAuthenticated()">
              <a class="btn btn-primary w-100 mt-3" href="${pageContext.request.contextPath}/login">Sign in to schedule or reserve</a>
            </security:authorize>
          </aside>
        </div>
      </div>
    </main>

    <%@ include file="components/footer.jsp" %>
  </body>
</html>
