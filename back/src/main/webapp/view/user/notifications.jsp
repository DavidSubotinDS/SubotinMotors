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
          <c:set var="accountNavActive" value="notifications" />
          <%@ include file="../components/user-sidebar.jsp" %>
          <div class="content-wrapper account-content-surface">
            <div class="d-flex justify-content-between align-items-center">
              <h2 class="fw-bold mb-3">Notifications</h2>
              <c:if test="${unreadNotificationCount > 0}">
                <form:form action="${pageContext.request.contextPath}/user/notifications/read-all" method="POST">
                  <button class="btn btn-sm btn-outline-secondary" type="submit">Mark all read</button>
                </form:form>
              </c:if>
            </div>
            <div class="list-group">
              <c:forEach items="${notifications}" var="notification">
                <div class="list-group-item ${notification.read ? '' : 'notification-unread'}">
                  <div class="d-flex justify-content-between gap-3">
                    <div>
                      <p class="mb-1 fw-semibold">${notification.message}</p>
                      <a href="<%= request.getContextPath() %>/cars/${notification.car.make}/${notification.car.model}/${notification.car.year}/${notification.car.idCar}">View auction</a>
                    </div>
                    <c:if test="${!notification.read}">
                      <form:form action="${pageContext.request.contextPath}/user/notifications/${notification.idNotification}/read" method="POST">
                        <button class="btn btn-sm btn-outline-primary" type="submit">Mark read</button>
                      </form:form>
                    </c:if>
                  </div>
                </div>
              </c:forEach>
              <c:if test="${empty notifications}">
                <p class="text-secondary">No notifications yet.</p>
              </c:if>
            </div>
          </div>
        </div>
      </div>
    </main>
    <%@ include file="../components/footer.jsp" %>
  </body>
</html>
