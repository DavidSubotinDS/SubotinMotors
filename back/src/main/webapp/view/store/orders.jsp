<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/store.css" />
    <link rel="stylesheet" href="/css/user.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main class="account-main">
      <div class="container py-4 py-lg-5">
        <div class="account-shell">
          <c:set var="accountNavActive" value="orders" />
          <%@ include file="../components/user-sidebar.jsp" %>
          <div class="account-content account-content-surface">
            <c:if test="${not empty storeMessage}"><div class="alert alert-info">${storeMessage}</div></c:if>
            <div class="d-flex flex-wrap gap-3 justify-content-between align-items-center mb-4">
              <h1 class="fw-bold mb-0">My parts orders</h1>
              <a class="btn btn-primary" href="${pageContext.request.contextPath}/parts">Continue shopping</a>
            </div>
            <div class="table-responsive">
              <table class="table align-middle">
          <thead><tr><th>Order</th><th>Date</th><th>Items</th><th>Total</th><th>Status</th><th></th></tr></thead>
          <tbody>
            <c:forEach items="${orders}" var="order">
              <tr>
                <td>#${order.idOrder}</td>
                <td>${order.createdAt}</td>
                <td>${order.items.size()}</td>
                <td><fmt:formatNumber value="${order.totalMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" /> ${order.currency.toUpperCase()}</td>
                <td>
                  <span class="order-status ${order.status eq 'PAID' ? 'order-status-paid' : (order.status eq 'PAYMENT_FAILED' || order.status eq 'EXPIRED' ? 'order-status-failed' : 'order-status-pending')}">${order.status}</span>
                </td>
                <td><a class="btn btn-sm btn-outline-primary" href="${pageContext.request.contextPath}/orders/${order.idOrder}">Details</a></td>
              </tr>
            </c:forEach>
            <c:if test="${empty orders}"><tr><td colspan="6" class="text-center py-5 text-secondary">You have not placed any parts orders yet.</td></tr></c:if>
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
