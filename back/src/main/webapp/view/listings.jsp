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
      <section class="listing-hero p-4 p-md-5 mb-4">
        <div class="row align-items-center g-4">
          <div class="col-lg-8">
            <span class="text-uppercase fw-semibold">Fixed-price marketplace</span>
            <h1 class="fw-bold mt-2">Cars listed by local sellers</h1>
            <p class="mb-0">Browse at a listed price, request a test ride, and reserve a car with a separate deposit.</p>
          </div>
          <div class="col-lg-4 text-lg-end">
            <a class="btn btn-warning" href="${pageContext.request.contextPath}/user/listings/new">List your car</a>
          </div>
        </div>
      </section>

      <form action="${pageContext.request.contextPath}/listings" class="row g-2 align-items-end mb-4">
        <div class="col-md-5">
          <label class="form-label" for="keyword">Search listings</label>
          <input class="form-control" id="keyword" name="keyword" value="${keyword}" placeholder="Title, make, model, or year" />
        </div>
        <div class="col-md-3">
          <label class="form-label" for="sort">Sort by</label>
          <select class="form-select" id="sort" name="sort">
            <option value="createdAt" ${sort eq 'createdAt' ? 'selected' : ''}>Newest</option>
            <option value="priceMinor" ${sort eq 'priceMinor' ? 'selected' : ''}>Price</option>
            <option value="make" ${sort eq 'make' ? 'selected' : ''}>Make</option>
            <option value="year" ${sort eq 'year' ? 'selected' : ''}>Year</option>
            <option value="mileage" ${sort eq 'mileage' ? 'selected' : ''}>Mileage</option>
          </select>
        </div>
        <div class="col-md-2">
          <label class="form-label" for="direction">Direction</label>
          <select class="form-select" id="direction" name="direction">
            <option value="desc" ${direction eq 'desc' ? 'selected' : ''}>Descending</option>
            <option value="asc" ${direction eq 'asc' ? 'selected' : ''}>Ascending</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-primary w-100" type="submit">Apply</button>
        </div>
      </form>

      <div class="row g-4">
        <c:forEach items="${listings}" var="listing">
          <div class="col-md-6 col-xl-4">
            <article class="listing-card">
              <c:choose>
                <c:when test="${not empty listing.picture}">
                  <img src="data:${listing.picture.fileType};base64,${listing.picture.image}" alt="${listing.title}" />
                </c:when>
                <c:otherwise>
                  <div class="listing-placeholder"><i class="fa-solid fa-car"></i></div>
                </c:otherwise>
              </c:choose>
              <div class="p-4">
                <div class="d-flex justify-content-between gap-3">
                  <div>
                    <h2 class="h5 fw-bold mb-1">${listing.title}</h2>
                    <p class="text-secondary mb-2">${listing.make} ${listing.model} &middot; ${listing.year}</p>
                  </div>
                  <span class="badge ${listing.active ? 'bg-success' : 'bg-warning text-dark'}">${listing.status.label}</span>
                </div>
                <div class="listing-specs mb-3">
                  <span><i class="fa-solid fa-road"></i> <fmt:formatNumber value="${listing.mileage}" /> km</span>
                  <span><i class="fa-solid fa-gas-pump"></i> ${listing.fuelType}</span>
                  <span><i class="fa-solid fa-gears"></i> ${listing.transmission}</span>
                </div>
                <p class="listing-price mb-3"><fmt:formatNumber value="${listing.priceAmount}" minFractionDigits="2" maxFractionDigits="2" /> EUR</p>
                <a class="btn btn-primary w-100" href="${pageContext.request.contextPath}/listings/${listing.idListing}">View listing</a>
              </div>
            </article>
          </div>
        </c:forEach>
        <c:if test="${empty listings}">
          <div class="col-12">
            <div class="text-center border rounded-4 p-5">
              <h2 class="h4">No matching car listings</h2>
              <p class="text-secondary mb-0">Try a broader search or add the first listing.</p>
            </div>
          </div>
        </c:if>
      </div>

      <c:if test="${listingPage.totalPages > 1}">
        <nav class="mt-4" aria-label="Listing pages">
          <ul class="pagination justify-content-center">
            <c:url var="previousUrl" value="/listings">
              <c:param name="page" value="${listingPage.number - 1}" />
              <c:param name="sort" value="${sort}" />
              <c:param name="direction" value="${direction}" />
              <c:if test="${not empty keyword}"><c:param name="keyword" value="${keyword}" /></c:if>
            </c:url>
            <li class="page-item ${listingPage.first ? 'disabled' : ''}">
              <a class="page-link" href="${listingPage.first ? '#' : previousUrl}">Previous</a>
            </li>
            <li class="page-item disabled"><span class="page-link">${listingPage.number + 1} / ${listingPage.totalPages}</span></li>
            <c:url var="nextUrl" value="/listings">
              <c:param name="page" value="${listingPage.number + 1}" />
              <c:param name="sort" value="${sort}" />
              <c:param name="direction" value="${direction}" />
              <c:if test="${not empty keyword}"><c:param name="keyword" value="${keyword}" /></c:if>
            </c:url>
            <li class="page-item ${listingPage.last ? 'disabled' : ''}">
              <a class="page-link" href="${listingPage.last ? '#' : nextUrl}">Next</a>
            </li>
          </ul>
        </nav>
      </c:if>
    </main>

    <%@ include file="components/footer.jsp" %>
  </body>
</html>
