<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/admin.css" />
  </head>
  <body>
    <!-- Navbar -->
    <%@ include file="../components/navbar.jsp" %>

    <!-- Main -->
    <main>
      <div class="container pt-5">
        <div class="d-flex">
          <!-- Sidebar -->
          <aside class="sidebar-admin pe-md-3">
            <ul>
              <li>
                <a href="<%= request.getContextPath() %>/admin"><i class="fa-solid fa-gauge-high"></i> Dashboard</a>
              </li>
              <li class="active-page">
                <a href="<%= request.getContextPath() %>/admin/car-management"><i class="fa-solid fa-car"></i> Car Management</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/admin/transactions"><i class="fa-solid fa-receipt"></i> Transactions</a>
              </li>
            </ul>
          </aside>

          <!-- Content -->
          <div class="content-wrapper">
            <!-- List Car -->
            <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3">
              <h2 class="fw-bold mb-0 flex-grow-1">Car List</h2>
              <form action="${pageContext.request.contextPath}/admin/car-management" class="d-flex gap-2">
                <input type="hidden" name="bidPage" value="${bidPage.number}" />
                <input type="hidden" name="bidSort" value="${bidSort}" />
                <input type="hidden" name="bidDirection" value="${bidDirection}" />
                <select class="form-select" name="carSort" aria-label="Sort cars">
                  <option value="idCar" ${carSort eq 'idCar' ? 'selected' : ''}>ID</option>
                  <option value="make" ${carSort eq 'make' ? 'selected' : ''}>Make</option>
                  <option value="model" ${carSort eq 'model' ? 'selected' : ''}>Model</option>
                  <option value="year" ${carSort eq 'year' ? 'selected' : ''}>Year</option>
                  <option value="price" ${carSort eq 'price' ? 'selected' : ''}>Price</option>
                  <option value="status" ${carSort eq 'status' ? 'selected' : ''}>Status</option>
                </select>
                <select class="form-select" name="carDirection" aria-label="Car sort direction">
                  <option value="asc" ${carDirection eq 'asc' ? 'selected' : ''}>Ascending</option>
                  <option value="desc" ${carDirection eq 'desc' ? 'selected' : ''}>Descending</option>
                </select>
                <button class="btn btn-outline-secondary" type="submit">Sort</button>
              </form>
            </div>
            <div class="table-responsive-md">
              <table class="table table-striped">
                <!-- Head -->
                <thead>
                  <tr>
                    <th>Car Id</th>
                    <th>Make</th>
                    <th>Model</th>
                    <th>Year</th>
                    <th>Price</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <!-- Body -->
                <tbody>
                  <c:forEach items="${listCar}" var="car">
                    <tr>
                      <td>${car.idCar}</td>
                      <td>${car.make}</td>
                      <td>${car.model}</td>
                      <td>${car.year}</td>
                      <td>${car.price}</td>
                      <c:if test="${car.status.equals('ACTIVE')}">
                        <td class="fw-semibold text-primary">${car.status}</td>
                      </c:if>
                      <c:if test="${car.status.equals('DEACTIVE')}">
                        <td class="fw-semibold text-danger">${car.status}</td>
                      </c:if>
                      <c:if test="${car.status.equals('PENDING')}">
                        <td class="fw-semibold text-warning">${car.status}</td>
                      </c:if>
                      <c:if test="${car.status.equals('SOLD')}">
                        <td class="fw-semibold text-success">${car.status}</td>
                      </c:if>
                      <c:if test="${car.status.equals('RESERVED')}">
                        <td class="fw-semibold text-warning">${car.status}</td>
                      </c:if>
                      <c:if test="${!car.status.equals('SOLD') && !car.status.equals('RESERVED')}">
                        <td>
                          <div class="dropdown">
                            <button class="btn btn-warning dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="fa-regular fa-pen-to-square"></i></button>
                            <ul class="dropdown-menu dropdown-menu-dark">
                              <c:if test="${car.status.equals('ACTIVE')}">
                                <li>
                                  <form:form action="${pageContext.request.contextPath}/admin/deactivate/${car.idCar}" method="POST">
                                    <button class="dropdown-item" type="submit">Deactivate Car Post</button>
                                  </form:form>
                                </li>
                              </c:if>
                              <c:if test="${car.status.equals('DEACTIVE')}">
                                <li>
                                  <form:form action="${pageContext.request.contextPath}/admin/activate/${car.idCar}" method="POST">
                                    <button class="dropdown-item" type="submit">Activate Car Post</button>
                                  </form:form>
                                </li>
                              </c:if>
                              <c:if test="${car.status.equals('PENDING')}">
                                <li>
                                  <form:form action="${pageContext.request.contextPath}/admin/activate/${car.idCar}" method="POST">
                                    <button class="dropdown-item" type="submit">Approve Listing</button>
                                  </form:form>
                                </li>
                                <li>
                                  <form:form action="${pageContext.request.contextPath}/admin/deactivate/${car.idCar}" method="POST">
                                    <button class="dropdown-item" type="submit">Reject Listing</button>
                                  </form:form>
                                </li>
                              </c:if>
                            </ul>
                          </div>
                        </td>
                      </c:if>
                      <c:if test="${car.status.equals('SOLD')}">
                        <td></td>
                      </c:if>
                      <c:if test="${car.status.equals('RESERVED')}">
                        <td></td>
                      </c:if>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
            <c:if test="${carPage.totalPages > 1}">
              <nav aria-label="Car management pages">
                <ul class="pagination justify-content-end">
                  <c:url var="previousAdminCarsUrl" value="/admin/car-management">
                    <c:param name="carPage" value="${carPage.number - 1}" />
                    <c:param name="carSort" value="${carSort}" />
                    <c:param name="carDirection" value="${carDirection}" />
                    <c:param name="bidPage" value="${bidPage.number}" />
                    <c:param name="bidSort" value="${bidSort}" />
                    <c:param name="bidDirection" value="${bidDirection}" />
                  </c:url>
                  <li class="page-item ${carPage.first ? 'disabled' : ''}">
                    <a class="page-link" href="${carPage.first ? '#' : previousAdminCarsUrl}">Previous</a>
                  </li>
                  <li class="page-item disabled"><span class="page-link">${carPage.number + 1} / ${carPage.totalPages}</span></li>
                  <c:url var="nextAdminCarsUrl" value="/admin/car-management">
                    <c:param name="carPage" value="${carPage.number + 1}" />
                    <c:param name="carSort" value="${carSort}" />
                    <c:param name="carDirection" value="${carDirection}" />
                    <c:param name="bidPage" value="${bidPage.number}" />
                    <c:param name="bidSort" value="${bidSort}" />
                    <c:param name="bidDirection" value="${bidDirection}" />
                  </c:url>
                  <li class="page-item ${carPage.last ? 'disabled' : ''}">
                    <a class="page-link" href="${carPage.last ? '#' : nextAdminCarsUrl}">Next</a>
                  </li>
                </ul>
              </nav>
            </c:if>

            <!-- Car Bid -->
            <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-3 mt-5">
              <h2 class="fw-bold mb-0 flex-grow-1">Car Bid</h2>
              <form action="${pageContext.request.contextPath}/admin/car-management" class="d-flex gap-2">
                <input type="hidden" name="carPage" value="${carPage.number}" />
                <input type="hidden" name="carSort" value="${carSort}" />
                <input type="hidden" name="carDirection" value="${carDirection}" />
                <select class="form-select" name="bidSort" aria-label="Sort bids">
                  <option value="idBid" ${bidSort eq 'idBid' ? 'selected' : ''}>Bid ID</option>
                  <option value="car.make" ${bidSort eq 'car.make' ? 'selected' : ''}>Car make</option>
                  <option value="car.year" ${bidSort eq 'car.year' ? 'selected' : ''}>Car year</option>
                  <option value="bidPrice" ${bidSort eq 'bidPrice' ? 'selected' : ''}>Bid price</option>
                  <option value="status" ${bidSort eq 'status' ? 'selected' : ''}>Status</option>
                </select>
                <select class="form-select" name="bidDirection" aria-label="Bid sort direction">
                  <option value="asc" ${bidDirection eq 'asc' ? 'selected' : ''}>Ascending</option>
                  <option value="desc" ${bidDirection eq 'desc' ? 'selected' : ''}>Descending</option>
                </select>
                <button class="btn btn-outline-secondary" type="submit">Sort</button>
              </form>
            </div>
            <div class="table-responsive-md">
              <table class="table table-striped">
                <!-- Head -->
                <thead>
                  <tr>
                    <th>Car Id</th>
                    <th>Make</th>
                    <th>Model</th>
                    <th>Year</th>
                    <th>Bid Price</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <!-- Body -->
                <tbody>
                  <c:forEach items="${listCarBid}" var="bid">
                    <tr>
                      <td>${bid.car.idCar}</td>
                      <td>${bid.car.make}</td>
                      <td>${bid.car.model}</td>
                      <td>${bid.car.year}</td>
                      <td>${bid.bidPrice}</td>
                      <c:if test="${bid.status.equals('ONGOING')}">
                        <td class="fw-semibold text-primary">${bid.status}</td>
                      </c:if>
                      <c:if test="${bid.status.equals('APPROVED')}">
                        <td class="fw-semibold text-success">${bid.status}</td>
                      </c:if>
                      <c:if test="${bid.status.equals('ACCEPTED_PENDING_PAYMENT')}">
                        <td class="fw-semibold text-warning">${bid.status}</td>
                      </c:if>
                      <c:if test="${bid.status.equals('PAID')}">
                        <td class="fw-semibold text-success">${bid.status}</td>
                      </c:if>
                      <c:if test="${bid.status.equals('PAYMENT_FAILED') || bid.status.equals('EXPIRED')}">
                        <td class="fw-semibold text-danger">${bid.status}</td>
                      </c:if>
                      <c:if test="${bid.status.equals('DENIED')}">
                        <td class="fw-semibold text-danger">${bid.status}</td>
                      </c:if>
                      <c:if test="${bid.status.equals('CANCELLED')}">
                        <td class="fw-semibold text-secondary">${bid.status}</td>
                      </c:if>
                      <td>
                        <c:if test="${bid.status.equals('ONGOING')}">
                          <div class="dropdown">
                            <button class="btn btn-warning dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="fa-regular fa-pen-to-square"></i></button>
                            <ul class="dropdown-menu dropdown-menu-dark">
                              <li>
                                <form:form action="${pageContext.request.contextPath}/admin/approve-bid/${bid.idBid}" method="POST">
                                  <button class="dropdown-item" type="submit">Approve Bid</button>
                                </form:form>
                              </li>
                              <li>
                                <form:form action="${pageContext.request.contextPath}/admin/deny-bid/${bid.idBid}" method="POST">
                                  <button class="dropdown-item" type="submit">Deny Bid</button>
                                </form:form>
                              </li>
                            </ul>
                          </div>
                        </c:if>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
            <c:if test="${bidPage.totalPages > 1}">
              <nav aria-label="Bid management pages">
                <ul class="pagination justify-content-end">
                  <c:url var="previousBidsUrl" value="/admin/car-management">
                    <c:param name="bidPage" value="${bidPage.number - 1}" />
                    <c:param name="bidSort" value="${bidSort}" />
                    <c:param name="bidDirection" value="${bidDirection}" />
                    <c:param name="carPage" value="${carPage.number}" />
                    <c:param name="carSort" value="${carSort}" />
                    <c:param name="carDirection" value="${carDirection}" />
                  </c:url>
                  <li class="page-item ${bidPage.first ? 'disabled' : ''}">
                    <a class="page-link" href="${bidPage.first ? '#' : previousBidsUrl}">Previous</a>
                  </li>
                  <li class="page-item disabled"><span class="page-link">${bidPage.number + 1} / ${bidPage.totalPages}</span></li>
                  <c:url var="nextBidsUrl" value="/admin/car-management">
                    <c:param name="bidPage" value="${bidPage.number + 1}" />
                    <c:param name="bidSort" value="${bidSort}" />
                    <c:param name="bidDirection" value="${bidDirection}" />
                    <c:param name="carPage" value="${carPage.number}" />
                    <c:param name="carSort" value="${carSort}" />
                    <c:param name="carDirection" value="${carDirection}" />
                  </c:url>
                  <li class="page-item ${bidPage.last ? 'disabled' : ''}">
                    <a class="page-link" href="${bidPage.last ? '#' : nextBidsUrl}">Next</a>
                  </li>
                </ul>
              </nav>
            </c:if>
            <!-- List Admin -->
          </div>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
