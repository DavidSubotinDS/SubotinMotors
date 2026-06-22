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
              <li><a href="<%= request.getContextPath() %>/user"><i class="fa-solid fa-user"></i> Profile</a></li>
              <li><a href="<%= request.getContextPath() %>/user/bids"><i class="fa-solid fa-gavel"></i> My Bids</a></li>
              <li class="active-page"><a href="<%= request.getContextPath() %>/user/followed-auctions"><i class="fa-regular fa-star"></i> Followed Auctions</a></li>
              <li><a href="<%= request.getContextPath() %>/user/notifications"><i class="fa-regular fa-bell"></i> Notifications</a></li>
            </ul>
          </aside>
          <div class="content-wrapper">
            <h2 class="fw-bold mb-3">My Followed Auctions</h2>
            <c:if test="${followMessage != null}">
              <div class="alert alert-info">${followMessage}</div>
            </c:if>
            <div class="row">
              <c:forEach items="${follows}" var="follow">
                <div class="col-12 col-lg-6 mb-3">
                  <div class="card h-100 auction-card" data-auction-end="${follow.car.auctionEndTimeEpochMillis}">
                    <div class="card-body">
                      <h5>${follow.car.make} ${follow.car.model} (${follow.car.year})</h5>
                      <span class="badge ${follow.car.auctionStatus eq 'ENDING_SOON' ? 'bg-warning text-dark' : follow.car.auctionStatus eq 'ENDED' ? 'bg-secondary' : 'bg-success'}">${follow.car.auctionStatusLabel}</span>
                      <p class="auction-countdown fw-semibold mt-2">${follow.car.auctionEndTimeDisplay}</p>
                      <a class="btn btn-primary btn-sm" href="<%= request.getContextPath() %>/cars/${follow.car.make}/${follow.car.model}/${follow.car.year}/${follow.car.idCar}">View auction</a>
                      <form:form action="${pageContext.request.contextPath}/user/auctions/${follow.car.idCar}/unfollow" method="POST" cssClass="d-inline">
                        <input type="hidden" name="returnTo" value="followed" />
                        <button class="btn btn-outline-secondary btn-sm" type="submit">Unfollow</button>
                      </form:form>
                    </div>
                  </div>
                </div>
              </c:forEach>
              <c:if test="${empty follows}">
                <p class="text-secondary">You are not following any auctions yet.</p>
              </c:if>
            </div>
          </div>
        </div>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
    <script src="/js/auction-timer.js"></script>
  </body>
</html>
