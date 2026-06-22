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
                <form action="<%= request.getContextPath() %>/cars">
                  <input class="form-control mb-3 ps-4 pe-0" type="number" name="low" required placeholder="Minimum price" />
                  <input class="form-control mb-3 ps-4 pe-0" type="number" name="high" required placeholder="Maximum price" />
                  <button type="submit" class="btn btn-secondary">Search</button>
                </form>
              </li>
            </ul>
          </aside>

          <!-- Car List -->
          <div class="car-list">
            <!-- Search -->
            <form action="<%= request.getContextPath() %>/cars" id="searchForm" class="d-flex">
              <input class="form-control" type="text" name="keyword" required placeholder="Search by Make, Model, or Year" />
              <button type="submit" class="btn btn-light">
                <i class="fa-solid fa-magnifying-glass"></i>
              </button>
            </form>
            <!-- Car Details -->
            <div class="mt-4 row">
              <!-- Image -->
              <div class="car-image col-12 col-sm-8">
                <img class="img-fluid" alt="${car.make}" src="data:${car.carPicture.fileType};base64,${car.carPicture.image}" />
              </div>
              <!-- Details -->
              <div class="car-details col-12 col-sm-4">
                <h4 class="fw-bold">${car.make} ${car.model} ${car.year}</h4>
                <div class="auction-panel mb-3" data-auction-end="${car.auctionEndTimeEpochMillis}">
                  <span class="auction-status badge ${car.auctionStatus eq 'ENDING_SOON' ? 'bg-warning text-dark' : car.auctionStatus eq 'ENDED' ? 'bg-secondary' : car.auctionStatus eq 'SOLD' ? 'bg-success' : 'bg-primary'}">${car.auctionStatusLabel}</span>
                  <p class="auction-countdown fw-semibold mt-2 mb-0">${car.auctionEndTimeDisplay}</p>
                </div>
                <p class="text-secondary m-0">STARTING PRICE:</p>
                <p class="text-black fs-5">$${car.price}</p>
                <c:if test="${highestBidding != 0}">
                  <p class="text-secondary m-0">HIGHEST BID:</p>
                  <p class="text-black fs-5">$${highestBidding}</p>
                </c:if>
                <c:if test="${highestBidding == 0}">
                  <p class="text-secondary m-0">HIGHEST BID:</p>
                  <p class="text-black fs-5">$${highestBidding}</p>
                </c:if>
                <c:if test="${followMessage != null}">
                  <div class="alert alert-info py-2">${followMessage}</div>
                </c:if>
                <security:authorize access="isAuthenticated()">
                  <c:if test="${car.auctionOpen}">
                    <c:choose>
                      <c:when test="${following}">
                        <form:form action="${pageContext.request.contextPath}/user/auctions/${car.idCar}/unfollow" method="POST" cssClass="mb-2">
                          <button class="btn btn-outline-secondary" type="submit"><i class="fa-solid fa-star"></i> Unfollow auction</button>
                        </form:form>
                      </c:when>
                      <c:otherwise>
                        <form:form action="${pageContext.request.contextPath}/user/auctions/${car.idCar}/follow" method="POST" cssClass="mb-2">
                          <button class="btn btn-outline-secondary" type="submit"><i class="fa-regular fa-star"></i> Follow auction</button>
                        </form:form>
                      </c:otherwise>
                    </c:choose>
                  </c:if>
                </security:authorize>
                <div class="car-button-actions">
                  <c:choose>
                    <c:when test="${car.auctionOpen}">
                      <a class="text-decoration-none" href="<%= request.getContextPath() %>/car-bid?id=${car.idCar}">
                        <button class="btn btn-primary">Place Bid</button>
                      </a>
                      <a class="text-decoration-none" href="<%= request.getContextPath() %>/test-drive/${car.idCar}">
                        <button class="btn btn-warning">Schedule a Test Drive</button>
                      </a>
                    </c:when>
                    <c:otherwise>
                      <p class="text-secondary fw-semibold">This auction is no longer accepting bids.</p>
                    </c:otherwise>
                  </c:choose>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="components/footer.jsp" %>
    <script src="/js/auction-timer.js"></script>
  </body>
</html>

<!-- <main>
  <p>${car.make}</p>
  <p>${car.model}</p>
  <p>${car.year}</p>
  <p>${car.price}</p>
  <img alt="${car.carPicture.fileName}" src="data:${car.carPicture.fileType};base64,${car.carPicture.image}" />
  <c:if test="${highestBidding != 0}">
    <p>Highest Bid</p>
    <p>${highestBidding}</p>
  </c:if>

  <a href="<%= request.getContextPath() %>/car-bid?id=${car.idCar}">Place Bid</a>
  <a href="<%= request.getContextPath() %>/test-drive/${car.idCar}">Test Drive</a>
</main> -->
