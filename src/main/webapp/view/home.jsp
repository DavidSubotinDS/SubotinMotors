<%@ include file="components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="components/header.jsp" %>
    <link rel="stylesheet" href="/css/home.css" />
  </head>
  <body>
    <%@ include file="components/navbar.jsp" %>

    <main>
      <div class="container-fluid jumbotron">
        <div class="container d-flex justify-content-center align-items-center">
          <div class="jumbotron-content pt-4">
            <form id="searchForm" action="cars" class="d-flex">
              <input class="form-control" type="text" name="keyword" required placeholder="Search by Make, Model, or Year" />
              <button type="submit" class="btn btn-light">
                <i class="fa-solid fa-magnifying-glass"></i>
              </button>
            </form>
            <div class="jumbotron-text">
              <h2 class="fw-bolder m-0">Affordable</h2>
              <h2 class="fw-bolder">and Like New</h2>
              <p>Timed auctions, fixed-price listings, test rides and reservations</p>
            </div>
            <a class="btn-search btn btn-outline-light text-uppercase mt-3" href="<%= request.getContextPath() %>/cars">Search Cars</a>
          </div>
        </div>
      </div>

      <div class="container mt-4">
        <div class="row align-items-center bg-light rounded-4 p-4 p-md-5">
          <div class="col-md-8">
            <span class="text-uppercase text-primary fw-semibold">Fixed-price marketplace</span>
            <h2 class="fw-bold mt-2">Browse cars without an auction</h2>
            <p class="text-secondary mb-md-0">Contact third-party sellers through test-ride requests and reserve a listed car with a separate deposit.</p>
          </div>
          <div class="col-md-4 text-md-end">
            <a class="btn btn-primary" href="<%= request.getContextPath() %>/listings">Browse listings</a>
          </div>
        </div>
      </div>

      <div class="container mt-4 p-3">
        <div class="d-flex justify-content-between">
          <h2 class="fw-bolder">Featured Cars</h2>
          <a class="text-decoration-none" href="<%= request.getContextPath() %>/cars">See all cars</a>
        </div>
        <div class="wrapper row">
          <c:forEach items="${listCar}" var="car">
            <div class="col-12 col-md-6 col-lg-4 mb-3">
              <div class="card auction-card" data-auction-end="${car.auctionEndTimeEpochMillis}">
                <img class="card-img-top" src="data:${car.carPicture.fileType};base64,${car.carPicture.image}" alt="${car.make}" />
                <div class="card-body">
                  <p class="car-details fw-bold">${car.make} ${car.model} ${car.year}</p>
                  <p class="car-price">$${car.price}</p>
                  <span class="badge ${car.auctionStatus eq 'ENDING_SOON' ? 'bg-warning text-dark' : car.auctionStatus eq 'ENDED' ? 'bg-secondary' : 'bg-success'}">${car.auctionStatusLabel}</span>
                  <p class="auction-countdown small fw-semibold mt-2">${car.auctionEndTimeDisplay}</p>
                  <a class="btn btn-primary" href="<%= request.getContextPath() %>/cars/${car.make}/${car.model}/${car.year}/${car.idCar}">Car Details</a>
                </div>
              </div>
            </div>
          </c:forEach>
        </div>
      </div>

      <div class="container mt-4">
        <div class="row align-items-center bg-light rounded-4 p-4 p-md-5">
          <div class="col-md-8">
            <span class="text-uppercase text-primary fw-semibold">Autostrada Parts</span>
            <h2 class="fw-bold mt-2">Maintain your car with confidence</h2>
            <p class="text-secondary mb-md-0">Browse filters, brakes, batteries, lighting, fluids and roadside accessories, then purchase the complete cart through secure sandbox checkout.</p>
          </div>
          <div class="col-md-4 text-md-end">
            <a class="btn btn-primary" href="<%= request.getContextPath() %>/parts">Shop car parts</a>
          </div>
        </div>
      </div>

      <div class="container mt-4">
        <div class="sell-your-car text-center">
          <h3 class="fw-bold">Sell Your Car For The Best Price</h3>
          <c:choose>
            <c:when test="${not empty sessionScope.SPRING_SECURITY_CONTEXT}">
              <a class="btn btn-outline-light mb-3 mt-2" href="<%= request.getContextPath() %>/user/listings/new">List Car Now</a>
            </c:when>
            <c:otherwise>
              <a class="btn btn-outline-light mb-3 mt-2" href="<%= request.getContextPath() %>/login">Sell Car Now</a>
            </c:otherwise>
          </c:choose>
          <p class="text-uppercase m-0">100% Verified Buyers</p>
        </div>
      </div>
    </main>

    <%@ include file="components/footer.jsp" %>
    <script src="/js/auction-timer.js"></script>
  </body>
</html>
