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
              <li class="active-page">
                <a href="<%= request.getContextPath() %>/user/bids"><i class="fa-solid fa-gavel"></i> My Bids</a>
              </li>
              <li>
                <a href="<%= request.getContextPath() %>/user/test-drive"><i class="fa-regular fa-calendar-check"></i> Appointments</a>
              </li>
            </ul>
          </aside>

          <div class="content-wrapper">
            <h2 class="fw-bold mb-3">My Bids</h2>

            <c:if test="${bidMessage != null}">
              <div class="alert alert-success">${bidMessage}</div>
            </c:if>
            <c:if test="${bidError != null}">
              <div class="alert alert-danger">${bidError}</div>
            </c:if>

            <div class="table-responsive-md">
              <table class="table table-striped align-middle">
                <thead>
                  <tr>
                    <th>Car</th>
                    <th>Bid</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach items="${bids}" var="bid">
                    <tr>
                      <td>
                        <a href="<%= request.getContextPath() %>/cars/${bid.car.make}/${bid.car.model}/${bid.car.year}/${bid.car.idCar}">
                          ${bid.car.make} ${bid.car.model} (${bid.car.year})
                        </a>
                      </td>
                      <td>${bid.bidPrice}</td>
                      <td>${bid.status}</td>
                      <td>
                        <c:if test="${bid.status eq 'ONGOING'}">
                          <form:form action="${pageContext.request.contextPath}/user/bids/${bid.idBid}/cancel" method="POST">
                            <button class="btn btn-sm btn-outline-danger" type="submit">Cancel bid</button>
                          </form:form>
                        </c:if>
                      </td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty bids}">
                    <tr>
                      <td colspan="4" class="text-secondary">You have not placed any bids.</td>
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
