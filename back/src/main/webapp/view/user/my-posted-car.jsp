<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/user.css" />
  </head>
  <body>
    <!-- Navbar -->
    <%@ include file="../components/navbar.jsp" %>

    <!-- Main -->
    <main class="account-main">
      <div class="container py-4 py-lg-5">
        <div class="account-shell">
          <c:set var="accountNavActive" value="listings" />
          <%@ include file="../components/user-sidebar.jsp" %>

          <div class="content-wrapper account-content-surface">
            <h2 class="fw-bold mb-3">Posted Cars</h2>
            <!-- Table -->
            <c:if test="${!userCar.isEmpty()}">
              <div class="table-responsive-md">
                <table class="table table-striped">
                  <!-- Head -->
                  <thead>
                    <tr>
                      <th>Id Car</th>
                      <th>Make</th>
                      <th>Model</th>
                      <th>Year</th>
                      <th>Price</th>
                      <th>Auction</th>
                      <th>Status</th>
                      <th></th>
                    </tr>
                  </thead>
                  <!-- Body -->
                  <tbody>
                    <c:forEach items="${userCar}" var="car">
                      <tr>
                        <td>${car.idCar}</td>
                        <td>${car.make}</td>
                        <td>${car.model}</td>
                        <td>${car.year}</td>
                        <td>$${car.price}</td>
                        <td data-auction-end="${car.auctionEndTimeEpochMillis}">
                          <span class="auction-countdown small">${car.auctionEndTimeDisplay}</span>
                        </td>
                        <c:if test="${car.status.equals('ACTIVE')}">
                          <td class="fw-semibold text-primary">${car.auctionStatusLabel}</td>
                        </c:if>
                        <c:if test="${car.status.equals('DEACTIVE')}">
                          <td class="fw-semibold text-danger">${car.status}</td>
                        </c:if>
                        <c:if test="${car.status.equals('PENDING')}">
                          <td class="fw-semibold text-warning">${car.status} - awaiting admin approval</td>
                        </c:if>
                        <c:if test="${car.status.equals('RESERVED')}">
                          <td class="fw-semibold text-warning">${car.status}</td>
                        </c:if>
                        <c:if test="${car.status.equals('SOLD')}">
                          <td class="fw-semibold text-success">${car.status}</td>
                        </c:if>
                        <c:if test="${car.status.equals('SOLD')}">
                          <td></td>
                        </c:if>
                        <c:if test="${!car.status.equals('SOLD')}">
                          <td>
                            <div class="dropdown">
                              <button class="btn btn-warning dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="fa-regular fa-pen-to-square"></i></button>
                              <ul class="dropdown-menu dropdown-menu-dark">
                                <li>
                                  <a class="dropdown-item" href="<%= request.getContextPath() %>/cars/${car.make}/${car.model}/${car.year}/${car.idCar}">Car Details</a>
                                </li>
                                <li>
                                  <a class="dropdown-item" href="<%= request.getContextPath() %>/user/edit-posted-car?id=${car.idCar}">Edit Details</a>
                                </li>
                                <li>
                                  <a class="dropdown-item" href="<%= request.getContextPath() %>/user/upload-car-picture?idCar=${car.idCar}">Edit Picture</a>
                                </li>
                                <c:if test="${car.status.equals('ACTIVE')}">
                                  <li>
                                    <form:form action="${pageContext.request.contextPath}/user/deactivate/${car.idCar}" method="POST">
                                      <button class="dropdown-item" type="submit">Deactivate Post</button>
                                    </form:form>
                                  </li>
                                </c:if>
                                <c:if test="${car.status.equals('DEACTIVE')}">
                                  <li>
                                    <form:form action="${pageContext.request.contextPath}/user/activate/${car.idCar}" method="POST">
                                      <button class="dropdown-item" type="submit">Activate Post</button>
                                    </form:form>
                                  </li>
                                </c:if>
                              </ul>
                            </div>
                          </td>
                        </c:if>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:if>
            <c:if test="${userCar.isEmpty()}">
              <p class="text-secondary">You have not posted any cars yet. Want to sell your car?</p>
              <a href="<%= request.getContextPath() %>/user/post-car">
                <button class="btn btn-primary">Get Started</button>
              </a>
            </c:if>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="../components/footer.jsp" %>
    <script src="/js/auction-timer.js"></script>
  </body>
</html>
