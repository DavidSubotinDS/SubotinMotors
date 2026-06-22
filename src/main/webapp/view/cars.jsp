<%@ include file="components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="components/header.jsp" %>
    <link rel="stylesheet" href="/css/cars.css" />
  </head>
  <body>
    <!-- Navbar -->
    <%@ include file="components/navbar.jsp" %>

    <!-- Main -->
    <main>
      <div class="container pt-5">
        <div class="d-flex">
          <!-- Sidebar -->
          <aside class="sidebar-car me-sm-4 pt-3">
            <ul class="fw-semibold">
              <li class="ms-1">
                <a href="<%= request.getContextPath() %>/cars"><i class="fa-solid fa-car"></i> Car List</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/user/post-car">
                  <button class="btn btn-warning">Post a car</button>
                </a>
              </li>
              <li>
                <p class="ms-1"><i class="fa-solid fa-dollar-sign"></i> Price Range</p>
                <c:if test="${not empty searchError}">
                  <p class="error">${searchError}</p>
                </c:if>
                <form action="<%= request.getContextPath() %>/cars">
                  <input class="form-control mb-3 ps-4 pe-0" type="number" name="low" required value="${low}" placeholder="Minimum price" />
                  <input class="form-control mb-3 ps-4 pe-0" type="number" name="high" required value="${high}" placeholder="Maximum price" />
                  <c:if test="${not empty keyword}">
                    <input type="hidden" name="keyword" value="${keyword}" />
                  </c:if>
                  <button type="submit" class="btn btn-secondary">Search</button>
                </form>
              </li>
            </ul>
          </aside>

          <!-- Car List -->
          <div class="car-list">
            <!-- Search -->
            <form action="<%= request.getContextPath() %>/cars" id="searchForm" class="d-flex">
              <input class="form-control" type="text" name="keyword" required value="${keyword}" placeholder="Search by Make, Model, or Year" />
              <c:if test="${low != null}">
                <input type="hidden" name="low" value="${low}" />
              </c:if>
              <c:if test="${high != null}">
                <input type="hidden" name="high" value="${high}" />
              </c:if>
              <button type="submit" class="btn btn-light">
                <i class="fa-solid fa-magnifying-glass"></i>
              </button>
            </form>
            <form action="<%= request.getContextPath() %>/cars" class="row g-2 align-items-end mt-2">
              <c:if test="${not empty keyword}">
                <input type="hidden" name="keyword" value="${keyword}" />
              </c:if>
              <c:if test="${low != null}">
                <input type="hidden" name="low" value="${low}" />
              </c:if>
              <c:if test="${high != null}">
                <input type="hidden" name="high" value="${high}" />
              </c:if>
              <div class="col-sm-5 col-lg-3">
                <label class="form-label mb-1" for="sort">Sort by</label>
                <select class="form-select" id="sort" name="sort">
                  <option value="idCar" ${sort eq 'idCar' ? 'selected' : ''}>Newest listing</option>
                  <option value="make" ${sort eq 'make' ? 'selected' : ''}>Make</option>
                  <option value="model" ${sort eq 'model' ? 'selected' : ''}>Model</option>
                  <option value="year" ${sort eq 'year' ? 'selected' : ''}>Year</option>
                  <option value="price" ${sort eq 'price' ? 'selected' : ''}>Price</option>
                  <option value="auctionEndTime" ${sort eq 'auctionEndTime' ? 'selected' : ''}>Auction end time</option>
                </select>
              </div>
              <div class="col-sm-4 col-lg-3">
                <label class="form-label mb-1" for="direction">Direction</label>
                <select class="form-select" id="direction" name="direction">
                  <option value="asc" ${direction eq 'asc' ? 'selected' : ''}>Ascending</option>
                  <option value="desc" ${direction eq 'desc' ? 'selected' : ''}>Descending</option>
                </select>
              </div>
              <div class="col-sm-3 col-lg-2">
                <button class="btn btn-outline-secondary w-100" type="submit">Apply</button>
              </div>
            </form>
            <!-- List -->
            <div class="mt-4 row">
              <c:if test="${!listCar.isEmpty()}">
                <c:forEach items="${listCar}" var="car">
                  <c:if test="${car.status.equals('ACTIVE')}">
                    <div class="col-12 col-md-6 col-md-4 col-lg-3 mb-3">
                      <div class="card auction-card" data-auction-end="${car.auctionEndTimeEpochMillis}">
                        <img class="card-img-top" src="data:${car.carPicture.fileType};base64,${car.carPicture.image}" alt="${car.make}" />
                        <div class="card-body">
                          <p class="car-details fw-bold">${car.make} ${car.model} ${car.year}</p>
                          <p class="car-price">$${car.price}</p>
                          <span class="auction-status badge ${car.auctionStatus eq 'ENDING_SOON' ? 'bg-warning text-dark' : car.auctionStatus eq 'ENDED' ? 'bg-secondary' : 'bg-success'}">${car.auctionStatusLabel}</span>
                          <p class="auction-countdown small fw-semibold mt-2 mb-1">${car.auctionEndTimeDisplay}</p>
                          <a href="<%= request.getContextPath() %>/cars/${car.make}/${car.model}/${car.year}/${car.idCar}">
                            <button class="btn btn-primary">Car Details</button>
                          </a>
                        </div>
                      </div>
                    </div>
                  </c:if>
                </c:forEach>
              </c:if>
              <c:if test="${listCar.isEmpty()}">
                <h4 class="fw-bold text-secondary text-center">Car not found</h4>
              </c:if>
            </div>
            <c:if test="${carPage.totalPages > 1}">
              <nav aria-label="Car catalogue pages" class="mt-3">
                <ul class="pagination justify-content-center">
                  <c:url var="previousCarsUrl" value="/cars">
                    <c:param name="page" value="${carPage.number - 1}" />
                    <c:param name="sort" value="${sort}" />
                    <c:param name="direction" value="${direction}" />
                    <c:if test="${not empty keyword}"><c:param name="keyword" value="${keyword}" /></c:if>
                    <c:if test="${low != null}"><c:param name="low" value="${low}" /></c:if>
                    <c:if test="${high != null}"><c:param name="high" value="${high}" /></c:if>
                  </c:url>
                  <li class="page-item ${carPage.first ? 'disabled' : ''}">
                    <a class="page-link" href="${carPage.first ? '#' : previousCarsUrl}">Previous</a>
                  </li>
                  <li class="page-item disabled">
                    <span class="page-link">Page ${carPage.number + 1} of ${carPage.totalPages}</span>
                  </li>
                  <c:url var="nextCarsUrl" value="/cars">
                    <c:param name="page" value="${carPage.number + 1}" />
                    <c:param name="sort" value="${sort}" />
                    <c:param name="direction" value="${direction}" />
                    <c:if test="${not empty keyword}"><c:param name="keyword" value="${keyword}" /></c:if>
                    <c:if test="${low != null}"><c:param name="low" value="${low}" /></c:if>
                    <c:if test="${high != null}"><c:param name="high" value="${high}" /></c:if>
                  </c:url>
                  <li class="page-item ${carPage.last ? 'disabled' : ''}">
                    <a class="page-link" href="${carPage.last ? '#' : nextCarsUrl}">Next</a>
                  </li>
                </ul>
              </nav>
            </c:if>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="components/footer.jsp" %>
    <script src="/js/auction-timer.js"></script>
  </body>
</html>
