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
          <c:set var="accountNavActive" value="appointments" />
          <%@ include file="../components/user-sidebar.jsp" %>

          <div class="content-wrapper account-content-surface">
            <h2 class="fw-bold mb-3">Test Drives</h2>

            <c:if test="${appointmentMessage != null}">
              <div class="alert alert-success">${appointmentMessage}</div>
            </c:if>
            <c:if test="${appointmentError != null}">
              <div class="alert alert-danger">${appointmentError}</div>
            </c:if>

            <h3 class="h5 mt-4">My bookings</h3>
            <div class="table-responsive-md mb-5">
              <table class="table table-striped align-middle">
                <thead>
                  <tr>
                    <th>Car</th>
                    <th>Seller</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Management</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach items="${bookedTestDrives}" var="test">
                    <tr>
                      <td>
                        <a href="<%= request.getContextPath() %>/cars/${test.car.make}/${test.car.model}/${test.car.year}/${test.car.idCar}">
                          ${test.car.make} ${test.car.model} (${test.car.year})
                        </a>
                      </td>
                      <td>${test.car.user.profile.firstName} ${test.car.user.profile.lastName}</td>
                      <td>${test.date}</td>
                      <td>
                        <c:choose>
                          <c:when test="${test.pending}">
                            <span class="badge bg-warning text-dark">${test.status.label}</span>
                          </c:when>
                          <c:when test="${test.accepted}">
                            <span class="badge bg-success">${test.status.label}</span>
                          </c:when>
                          <c:when test="${test.rejected}">
                            <span class="badge bg-danger">${test.status.label}</span>
                          </c:when>
                          <c:otherwise>
                            <span class="badge bg-secondary">${test.status.label}</span>
                          </c:otherwise>
                        </c:choose>
                      </td>
                      <td>
                        <c:choose>
                          <c:when test="${test.reschedulable}">
                            <div class="d-flex flex-wrap gap-2">
                              <form:form action="${pageContext.request.contextPath}/user/test-drives/${test.idTestDrive}/reschedule" method="POST" class="d-flex gap-2">
                                <input class="form-control form-control-sm" type="date" name="date" value="${test.date}" required />
                                <button class="btn btn-sm btn-outline-primary" type="submit">Reschedule</button>
                              </form:form>
                              <form:form action="${pageContext.request.contextPath}/user/test-drives/${test.idTestDrive}/cancel" method="POST">
                                <button class="btn btn-sm btn-outline-danger" type="submit">Cancel</button>
                              </form:form>
                            </div>
                          </c:when>
                          <c:otherwise>
                            <span class="text-secondary">No actions available</span>
                          </c:otherwise>
                        </c:choose>
                      </td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty bookedTestDrives}">
                    <tr>
                      <td colspan="5" class="text-secondary">You have no test-drive bookings.</td>
                    </tr>
                  </c:if>
                </tbody>
              </table>
            </div>

            <h3 class="h5">Requests for my cars</h3>
            <div class="table-responsive-md">
              <table class="table table-striped align-middle">
                <thead>
                  <tr>
                    <th>Car</th>
                    <th>Client</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Management</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach items="${receivedTestDrives}" var="test">
                    <tr>
                      <td>
                        <a href="<%= request.getContextPath() %>/cars/${test.car.make}/${test.car.model}/${test.car.year}/${test.car.idCar}">
                          ${test.car.make} ${test.car.model} (${test.car.year})
                        </a>
                      </td>
                      <td>${test.user.profile.firstName} ${test.user.profile.lastName}</td>
                      <td>${test.date}</td>
                      <td>
                        <c:choose>
                          <c:when test="${test.pending}">
                            <span class="badge bg-warning text-dark">${test.status.label}</span>
                          </c:when>
                          <c:when test="${test.accepted}">
                            <span class="badge bg-success">${test.status.label}</span>
                          </c:when>
                          <c:when test="${test.rejected}">
                            <span class="badge bg-danger">${test.status.label}</span>
                          </c:when>
                          <c:otherwise>
                            <span class="badge bg-secondary">${test.status.label}</span>
                          </c:otherwise>
                        </c:choose>
                      </td>
                      <td>
                        <c:choose>
                          <c:when test="${test.pending}">
                            <div class="d-flex flex-wrap gap-2">
                              <form:form action="${pageContext.request.contextPath}/user/test-drives/${test.idTestDrive}/accept" method="POST">
                                <button class="btn btn-sm btn-success" type="submit">Accept</button>
                              </form:form>
                              <form:form action="${pageContext.request.contextPath}/user/test-drives/${test.idTestDrive}/reject" method="POST">
                                <button class="btn btn-sm btn-outline-danger" type="submit">Reject</button>
                              </form:form>
                            </div>
                          </c:when>
                          <c:when test="${test.accepted}">
                            <form:form action="${pageContext.request.contextPath}/user/test-drives/${test.idTestDrive}/owner-cancel" method="POST">
                              <button class="btn btn-sm btn-outline-danger" type="submit">Cancel appointment</button>
                            </form:form>
                          </c:when>
                          <c:otherwise>
                            <span class="text-secondary">Decision recorded</span>
                          </c:otherwise>
                        </c:choose>
                      </td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty receivedTestDrives}">
                    <tr>
                      <td colspan="5" class="text-secondary">No one has requested a test drive for your cars.</td>
                    </tr>
                  </c:if>
                </tbody>
              </table>
            </div>

            <hr class="my-5" />
            <h2 class="fw-bold mb-3">Fixed-price listing test rides</h2>
            <h3 class="h5 mt-4">My listing bookings</h3>
            <div class="table-responsive-md mb-5">
              <table class="table table-striped align-middle">
                <thead><tr><th>Listing</th><th>Seller</th><th>Date and time</th><th>Status</th><th>Management</th></tr></thead>
                <tbody>
                  <c:forEach items="${listingTestRides}" var="test">
                    <tr>
                      <td><a href="${pageContext.request.contextPath}/listings/${test.listing.idListing}">${test.listing.title}</a></td>
                      <td>${test.listing.seller.profile.firstName} ${test.listing.seller.profile.lastName}</td>
                      <td>${test.scheduledAt}</td>
                      <td><span class="badge ${test.pending ? 'bg-warning text-dark' : test.accepted ? 'bg-success' : test.rejected ? 'bg-danger' : 'bg-secondary'}">${test.status.label}</span></td>
                      <td>
                        <c:choose>
                          <c:when test="${test.reschedulable}">
                            <div class="d-flex flex-wrap gap-2">
                              <form:form action="${pageContext.request.contextPath}/user/listing-test-rides/${test.idTestRide}/reschedule" method="POST" class="d-flex gap-2">
                                <input class="form-control form-control-sm" type="datetime-local" name="scheduledAt" value="${test.scheduledAt}" required />
                                <button class="btn btn-sm btn-outline-primary" type="submit">Reschedule</button>
                              </form:form>
                              <form:form action="${pageContext.request.contextPath}/user/listing-test-rides/${test.idTestRide}/cancel" method="POST"><button class="btn btn-sm btn-outline-danger" type="submit">Cancel</button></form:form>
                            </div>
                          </c:when>
                          <c:otherwise><span class="text-secondary">No actions available</span></c:otherwise>
                        </c:choose>
                      </td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty listingTestRides}"><tr><td colspan="5" class="text-secondary">You have no fixed-price listing test rides.</td></tr></c:if>
                </tbody>
              </table>
            </div>

            <h3 class="h5">Requests for my fixed-price listings</h3>
            <div class="table-responsive-md">
              <table class="table table-striped align-middle">
                <thead><tr><th>Listing</th><th>Interested user</th><th>Date and time</th><th>Status</th><th>Management</th></tr></thead>
                <tbody>
                  <c:forEach items="${listingTestRideRequests}" var="test">
                    <tr>
                      <td><a href="${pageContext.request.contextPath}/listings/${test.listing.idListing}">${test.listing.title}</a></td>
                      <td>${test.user.profile.firstName} ${test.user.profile.lastName}</td>
                      <td>${test.scheduledAt}</td>
                      <td><span class="badge ${test.pending ? 'bg-warning text-dark' : test.accepted ? 'bg-success' : test.rejected ? 'bg-danger' : 'bg-secondary'}">${test.status.label}</span></td>
                      <td>
                        <c:choose>
                          <c:when test="${test.pending}">
                            <div class="d-flex gap-2">
                              <form:form action="${pageContext.request.contextPath}/user/listing-test-rides/${test.idTestRide}/accept" method="POST"><button class="btn btn-sm btn-success" type="submit">Accept</button></form:form>
                              <form:form action="${pageContext.request.contextPath}/user/listing-test-rides/${test.idTestRide}/reject" method="POST"><button class="btn btn-sm btn-outline-danger" type="submit">Reject</button></form:form>
                            </div>
                          </c:when>
                          <c:when test="${test.accepted}">
                            <form:form action="${pageContext.request.contextPath}/user/listing-test-rides/${test.idTestRide}/owner-cancel" method="POST"><button class="btn btn-sm btn-outline-danger" type="submit">Cancel appointment</button></form:form>
                          </c:when>
                          <c:otherwise><span class="text-secondary">Decision recorded</span></c:otherwise>
                        </c:choose>
                      </td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty listingTestRideRequests}"><tr><td colspan="5" class="text-secondary">No test-ride requests for your fixed-price listings.</td></tr></c:if>
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
