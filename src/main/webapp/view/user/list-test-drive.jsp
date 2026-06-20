<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/user.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>

    <main>
      <div class="container pt-5">
        <div class="d-flex">
          <aside class="sidebar-user pe-md-3">
            <ul>
              <li>
                <a href="<%= request.getContextPath() %>/user"><i class="fa-solid fa-user"></i> Profile</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/user/my-posted-car"><i class="fa-solid fa-car"></i> My Posted Car</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/user/bids"><i class="fa-solid fa-gavel"></i> My Bids</a>
              </li>
              <li class="active-page">
                <a href="<%= request.getContextPath() %>/user/test-drive"><i class="fa-regular fa-calendar-check"></i> Appointments</a>
              </li>
            </ul>
          </aside>

          <div class="content-wrapper">
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
                        <div class="d-flex flex-wrap gap-2">
                          <form:form action="${pageContext.request.contextPath}/user/test-drives/${test.idTestDrive}/reschedule" method="POST" class="d-flex gap-2">
                            <input class="form-control form-control-sm" type="date" name="date" value="${test.date}" required />
                            <button class="btn btn-sm btn-outline-primary" type="submit">Reschedule</button>
                          </form:form>
                          <form:form action="${pageContext.request.contextPath}/user/test-drives/${test.idTestDrive}/cancel" method="POST">
                            <button class="btn btn-sm btn-outline-danger" type="submit">Cancel</button>
                          </form:form>
                        </div>
                      </td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty bookedTestDrives}">
                    <tr>
                      <td colspan="4" class="text-secondary">You have no test-drive bookings.</td>
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
                    </tr>
                  </c:forEach>
                  <c:if test="${empty receivedTestDrives}">
                    <tr>
                      <td colspan="3" class="text-secondary">No one has requested a test drive for your cars.</td>
                    </tr>
                  </c:if>
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
