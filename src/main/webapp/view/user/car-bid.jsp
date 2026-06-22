<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/form.css" />
    <link rel="stylesheet" href="/css/user.css" />
  </head>
  <body>
    <!-- Navbar -->
    <%@ include file="../components/navbar.jsp" %>

    <!-- Main -->
    <main class="account-main">
      <div class="container py-4 py-lg-5">
        <div class="account-shell">
          <c:set var="accountNavActive" value="bids" />
          <%@ include file="../components/user-sidebar.jsp" %>
          <div class="account-content">
            <div class="form-wrapper small">
          <h2 class="form-header">Place Bid</h2>
          <div data-auction-end="${car.auctionEndTimeEpochMillis}">
            <span class="badge ${car.auctionStatus eq 'ENDING_SOON' ? 'bg-warning text-dark' : 'bg-primary'}">${car.auctionStatusLabel}</span>
            <p class="auction-countdown fw-semibold mt-2">${car.auctionEndTimeDisplay}</p>
          </div>
          <c:if test="${highestBidding == 0}">
            <p class="text-secondary fs-5 m-0">STARTING PRICE:</p>
            <p class="fs-5 m-0">$${car.price}</p>
          </c:if>

          <c:if test="${highestBidding != 0}">
            <p class="text-secondary fs-5 m-0 text-uppercase">Highest Bid:</p>
            <p class="fs-5 m-0">$${highestBidding}</p>
          </c:if>
          <!-- FORM -->
          <p class="error">${message}</p>
          <c:if test="${car.auctionOpen}">
          <form:form action="postCarBidding" method="POST" modelAttribute="carBidding">
            <input type="hidden" name="carId" value="${car.idCar}" />

            <label class="fs-6 form-label">Bid Price</label>
            <form:input class="form-control" type="number" path="bidPrice" cssErrorClass="error-border" />
            <form:errors path="bidPrice" cssClass="error" />

            <button class="btn btn-primary form-button" type="submit">Place Bid</button>
          </form:form>
          </c:if>
          <c:if test="${!car.auctionOpen}">
            <div class="alert alert-secondary">This auction has ended and no longer accepts bids.</div>
          </c:if>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="../components/footer.jsp" %>
    <script src="/js/auction-timer.js"></script>
  </body>
</html>
