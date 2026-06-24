<%@ include file="../components/taglib.jsp" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="../components/header.jsp" %>
    <link rel="stylesheet" href="/css/admin.css" />
    <link rel="stylesheet" href="/css/store.css" />
  </head>
  <body>
    <%@ include file="../components/navbar.jsp" %>
    <main>
      <div class="container pt-5">
        <div class="d-flex">
          <aside class="sidebar-admin pe-md-3">
            <ul>
              <li><a href="${pageContext.request.contextPath}/admin"><i class="fa-solid fa-gauge-high"></i> Dashboard</a></li>
              <li><a href="${pageContext.request.contextPath}/admin/car-management"><i class="fa-solid fa-car"></i> Car Management</a></li>
              <li><a href="${pageContext.request.contextPath}/admin/store/parts"><i class="fa-solid fa-gears"></i> Parts Inventory</a></li>
              <li class="active-page"><a href="${pageContext.request.contextPath}/admin/store/orders"><i class="fa-solid fa-box"></i> Store Orders</a></li>
              <li><a href="${pageContext.request.contextPath}/admin/transactions"><i class="fa-solid fa-clock-rotate-left"></i> Legacy Transactions</a></li>
            </ul>
          </aside>
          <div class="content-wrapper">
            <h2 class="fw-bold mb-3">Store orders</h2>
            <div class="table-responsive">
              <table class="table table-striped align-middle">
                <thead><tr><th>Order</th><th>Customer</th><th>Created</th><th>Items</th><th>Total</th><th>Status</th><th>Stripe ID</th><th></th></tr></thead>
                <tbody>
                  <c:forEach items="${orders}" var="order">
                    <tr>
                      <td>#${order.idOrder}</td>
                      <td>${order.user.username}<br /><small>${order.user.email}</small></td>
                      <td>${order.createdAt}</td>
                      <td>${order.items.size()}</td>
                      <td><fmt:formatNumber value="${order.totalMinor / 100.0}" minFractionDigits="2" maxFractionDigits="2" /> ${order.currency.toUpperCase()}</td>
                      <td><span class="order-status ${order.status eq 'PAID' ? 'order-status-paid' : (order.status eq 'PAYMENT_FAILED' || order.status eq 'EXPIRED' ? 'order-status-failed' : 'order-status-pending')}">${order.status}</span></td>
                      <td><small>${empty order.paymentIntentId ? order.checkoutSessionId : order.paymentIntentId}</small></td>
                      <td><a class="btn btn-sm btn-outline-primary" href="${pageContext.request.contextPath}/admin/store/orders/${order.idOrder}">Details</a></td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty orders}"><tr><td colspan="8" class="text-center py-5">No store orders yet.</td></tr></c:if>
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
